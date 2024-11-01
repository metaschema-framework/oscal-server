/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server.core.commands

import gov.nist.secauto.metaschema.schemagen.SchemaGenerationFeature
import gov.nist.secauto.metaschema.core.model.util.XmlUtil
import gov.nist.secauto.metaschema.core.model.util.JsonUtil
import gov.nist.secauto.metaschema.core.util.CollectionUtil
import gov.nist.secauto.metaschema.databind.codegen.ModuleCompilerHelper
import gov.nist.secauto.metaschema.databind.codegen.IProduction
import gov.nist.secauto.metaschema.databind.model.IBoundModule
import java.nio.charset.StandardCharsets
import org.json.JSONObject
import java.net.URI
import gov.nist.secauto.metaschema.cli.commands.AbstractValidateContentCommand;

import gov.nist.secauto.metaschema.core.configuration.DefaultConfiguration
import gov.nist.secauto.metaschema.cli.processor.CLIProcessor.CallingContext
import gov.nist.secauto.metaschema.cli.processor.ExitCode
import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import gov.nist.secauto.metaschema.cli.processor.InvalidArgumentException
import gov.nist.secauto.metaschema.cli.processor.command.ICommandExecutor
import gov.nist.secauto.metaschema.core.model.IModule
import gov.nist.secauto.metaschema.core.model.MetaschemaException
import gov.nist.secauto.metaschema.core.model.xml.ModuleLoader
import gov.nist.secauto.metaschema.core.model.IModuleLoader;
import gov.nist.secauto.metaschema.core.model.constraint.IConstraintSet
import gov.nist.secauto.metaschema.databind.DefaultBindingContext
import gov.nist.secauto.metaschema.databind.IBindingContext
import gov.nist.secauto.metaschema.schemagen.ISchemaGenerator
import gov.nist.secauto.metaschema.schemagen.ISchemaGenerator.SchemaFormat
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option
import gov.nist.secauto.metaschema.core.model.xml.ExternalConstraintsModulePostProcessor
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.transform.Source
import java.util.Collections

class ValidateMetaschemaContent : AbstractValidateContentCommand() {
    protected val LOGGER = LogManager.getLogger(this::class.java)

    companion object {
        private const val COMMAND = "validate-metaschema-content"
        private const val LIB_DIR = "lib"  // Directory for storing modules

        val METASCHEMA_OPTION: Option = Option.builder("m")
            .hasArg()
            .argName("FILE_OR_URL")
            .desc("Metaschema resource")
            .required()
            .build()
    }

    override fun getName(): String = COMMAND

    override fun getDescription(): String =
        "Verify that the provided resource is well-formed and valid to the provided Module-based model."

    override fun gatherOptions(): Collection<Option> = listOf(METASCHEMA_OPTION) + super.gatherOptions()

    override fun validateOptions(callingContext: CallingContext, cmdLine: CommandLine) {
        LOGGER.info("Validating command options")
        val extraArgs = cmdLine.argList
        if (extraArgs.isEmpty()) {
            LOGGER.warn("No file args detected")
        }else{
            LOGGER.info(" $extraArgs")
        }
        LOGGER.info("Command options validated successfully")
    }

    override fun newExecutor(callingContext: CallingContext, cmdLine: CommandLine): ICommandExecutor {
        return ICommandExecutor.using(callingContext, cmdLine) { ctx, cmd -> executeCommand(ctx, cmd) }
    }

    protected fun executeCommand(callingContext: CallingContext, cmdLine: CommandLine): ExitStatus {
        LOGGER.debug("Starting execution of ValidateContentUsingModuleCommand")

        return try {
            val bindingContext = DefaultBindingContext()
            val module = loadOrCreateModule(cmdLine, bindingContext)

            // Validate content based on the loaded module
            validateModuleContent(module)

            LOGGER.info("Validation completed successfully")
            ExitCode.OK.exit()
        } catch (ex: MetaschemaException) {
            LOGGER.error("Metaschema error occurred", ex)
            ExitCode.PROCESSING_ERROR.exit().withThrowable(ex)
        } catch (ex: IOException) {
            LOGGER.error("I/O error occurred", ex)
            ExitCode.IO_ERROR.exit().withThrowable(ex)
        }
    }

    private fun loadOrCreateModule(
        cmdLine: CommandLine,
        bindingContext: IBindingContext
    ): IModule {
        val metaschemaPath = cmdLine.getOptionValue(METASCHEMA_OPTION.opt)
        val moduleUri = Paths.get(metaschemaPath).toAbsolutePath().toUri()
        val namespace = "namespace-based-name"  // Replace with actual namespace identifier
        val libPath = Paths.get(LIB_DIR, "module_${namespace.hashCode()}.jar")
        
        Files.createDirectories(libPath.parent)
        
        return if (Files.exists(libPath)) {
            LOGGER.info("Loading existing module from $libPath")
            loadModule(moduleUri)
        } else {
            LOGGER.info("Creating and saving new module to $libPath")
            createAndSaveModule(moduleUri, libPath, bindingContext)
        }
    }

    @Throws(IOException::class, MetaschemaException::class)
    private fun loadModule(moduleUri: URI): IModule {
        val constraintSets = Collections.emptySet<IConstraintSet>()
        val postProcessor = ExternalConstraintsModulePostProcessor(constraintSets)
        val postProcessors: List<IModuleLoader.IModulePostProcessor> = listOf(postProcessor)
        val loader = ModuleLoader(postProcessors).apply {
            allowEntityResolution()
        }
        return loader.load(moduleUri)
    }

    private fun createAndSaveModule(
        moduleUri: URI,
        libPath: Path,
        bindingContext: IBindingContext
    ): IModule {
        LOGGER.info(moduleUri);
        val module = loadModule(moduleUri)
        // Compile the module using ModuleCompilerHelper
        val tempDir = getTempDir()
        val production: IProduction = ModuleCompilerHelper.compileMetaschema(module, tempDir)
        
        // Create a new classloader for the compiled classes
        val classLoader = ModuleCompilerHelper.newClassLoader(tempDir, this.javaClass.classLoader)
        
        // Get the generated module class from the production
        val moduleProduction = production.getModuleProduction(module)
        if (moduleProduction != null) {
            bindingContext.registerModule(moduleProduction as Class<out IBoundModule>)
        } else {
            throw IllegalStateException("Failed to generate module class for $module")
        }
        
        // Copy the compiled classes to the lib directory
        Files.walk(tempDir)
            .filter { Files.isRegularFile(it) }
            .forEach { source ->
                val target = libPath.resolve(tempDir.relativize(source))
                Files.createDirectories(target.parent)
                Files.copy(source, target)
            }
        
        return module
    }

    private fun validateModuleContent(module: IModule) {
        LOGGER.info("Validating module content")
        // Implement validation logic based on module
    }

    private fun getTempDir(): Path = Files.createTempDirectory("validation-").apply {
        toFile().deleteOnExit()
    }

    @Throws(IOException::class)
    private fun generateXmlSchema(module: IModule): List<Source> {
        val schemaFile = Files.createTempFile(getTempDir(), "schema-", ".xml")
        val configuration = DefaultConfiguration<SchemaGenerationFeature<*>>()
        ISchemaGenerator.generateSchema(module, schemaFile, SchemaFormat.XML, configuration)
        return listOf(XmlUtil.getStreamSource(schemaFile.toUri().toURL()))
    }

    @Throws(IOException::class)
    private fun generateJsonSchema(module: IModule): JSONObject {
        val schemaFile = Files.createTempFile(getTempDir(), "schema-", ".json")
        val configuration = DefaultConfiguration<SchemaGenerationFeature<*>>()
        ISchemaGenerator.generateSchema(module, schemaFile, SchemaFormat.JSON, configuration)
        return Files.newBufferedReader(schemaFile, StandardCharsets.UTF_8).use { reader ->
            JsonUtil.toJsonObject(reader)
        }
    }
}
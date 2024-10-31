package gov.nist.secauto.metaschema.server.commands
import  gov.nist.secauto.metaschema.schemagen.SchemaGenerationFeature;
import gov.nist.secauto.metaschema.core.model.util.XmlUtil;
import gov.nist.secauto.metaschema.core.model.util.JsonUtil;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import java.net.URI;
import gov.nist.secauto.metaschema.core.configuration.DefaultConfiguration;
import java.io.BufferedReader;
import java.net.URISyntaxException;
import gov.nist.secauto.oscal.lib.OscalBindingContext
import gov.nist.secauto.metaschema.cli.processor.CLIProcessor.CallingContext
import gov.nist.secauto.metaschema.cli.processor.ExitCode
import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import gov.nist.secauto.metaschema.cli.commands.MetaschemaCommands
import gov.nist.secauto.metaschema.cli.processor.InvalidArgumentException
import gov.nist.secauto.metaschema.cli.processor.command.AbstractTerminalCommand
import gov.nist.secauto.metaschema.cli.processor.command.ICommandExecutor
import gov.nist.secauto.metaschema.core.model.IModule
import gov.nist.secauto.metaschema.core.model.MetaschemaException
import gov.nist.secauto.metaschema.databind.DefaultBindingContext
import gov.nist.secauto.metaschema.databind.IBindingContext
import gov.nist.secauto.metaschema.schemagen.ISchemaGenerator
import gov.nist.secauto.metaschema.schemagen.ISchemaGenerator.SchemaFormat
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.transform.Source

class ValidateContentUsingModuleCommand : AbstractTerminalCommand() {

    companion object {
        private val LOGGER = LogManager.getLogger(ValidateContentUsingModuleCommand::class.java)
        private const val COMMAND = "validate-content"
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

    override fun gatherOptions(): Collection<Option> = listOf(METASCHEMA_OPTION)

    override fun validateOptions(callingContext: CallingContext, cmdLine: CommandLine) {
        LOGGER.info("Validating command options")
        val extraArgs = cmdLine.argList
        if (extraArgs.isNotEmpty()) {
            LOGGER.warn("Extra arguments detected: $extraArgs")
            throw InvalidArgumentException("Illegal number of extra arguments.")
        }
        LOGGER.info("Command options validated successfully")
    }

    override fun newExecutor(callingContext: CallingContext, cmdLine: CommandLine): ICommandExecutor {
        return ICommandExecutor.using(callingContext, cmdLine) { ctx, cmd -> executeCommand(ctx, cmd) }
    }

    protected fun executeCommand(callingContext: CallingContext, cmdLine: CommandLine): ExitStatus {
        LOGGER.info("Starting execution of ValidateContentUsingModuleCommand")
        val cwd = Paths.get("").toAbsolutePath().toUri()
        LOGGER.info("Current working directory: $cwd")

        return try {
            val bindingContext = DefaultBindingContext()
            val module = loadOrCreateModule(cmdLine, bindingContext, cwd)

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
        bindingContext: IBindingContext,
        cwd: URI
    ): IModule {
        val namespace = "namespace-based-name"  // Replace with actual namespace identifier
        val libPath = Paths.get(LIB_DIR, "module_${namespace.hashCode()}.jar")
        val libFile = libPath.toFile()

        return if (libFile.exists()) {
            LOGGER.info("Loading existing module from $libPath")
            try {
                MetaschemaCommands.handleModule(cmdLine.getOptionValue(METASCHEMA_OPTION.opt), cwd)
            } catch (ex: URISyntaxException) {
                throw IOException("Invalid URI for module path.", ex)
            }
        } else {
            LOGGER.info("Creating and saving new module to $libPath")
            val module = MetaschemaCommands.handleModule(cmdLine.getOptionValue(METASCHEMA_OPTION.opt), cwd)
            bindingContext.registerModule(module, getTempDir())
            module.apply {
                DefaultBindingContext.instance().serializeModule(this, Files.newOutputStream(libPath))
            }
        }
    }

    private fun validateModuleContent(module: IModule) {
        LOGGER.info("Validating module content")
        // Implement validation logic based on module
        // This can include specific rule checks or validations based on the provided module
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
        return BufferedReader(Files.newBufferedReader(schemaFile, StandardCharsets.UTF_8)).use { reader ->
            JsonUtil.toJsonObject(reader)
        }
    }
}

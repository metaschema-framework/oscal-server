/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server.core.commands
import gov.nist.secauto.metaschema.core.metapath.item.JsonItemWriter;
import gov.nist.secauto.metaschema.core.metapath.item.IItem;
import gov.nist.secauto.oscal.lib.OscalModelConstants;
import gov.nist.secauto.oscal.lib.model.OscalCompleteModule
import gov.nist.secauto.oscal.lib.OscalBindingContext
import gov.nist.secauto.metaschema.cli.commands.MetaschemaCommands
import gov.nist.secauto.metaschema.cli.processor.CLIProcessor.CallingContext
import gov.nist.secauto.metaschema.cli.processor.ExitCode
import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import gov.nist.secauto.metaschema.cli.processor.InvalidArgumentException
import gov.nist.secauto.metaschema.cli.processor.command.AbstractTerminalCommand
import gov.nist.secauto.metaschema.cli.processor.command.ExtraArgument
import gov.nist.secauto.metaschema.cli.processor.command.ICommandExecutor
import gov.nist.secauto.metaschema.core.metapath.DynamicContext
import gov.nist.secauto.metaschema.core.metapath.ISequence
import gov.nist.secauto.metaschema.core.metapath.MetapathExpression
import gov.nist.secauto.metaschema.core.metapath.StaticContext
import gov.nist.secauto.metaschema.core.metapath.item.DefaultItemWriter
import gov.nist.secauto.metaschema.core.metapath.item.IItemWriter
import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItem
import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItemFactory
import gov.nist.secauto.metaschema.core.model.IModule
import gov.nist.secauto.metaschema.core.model.MetaschemaException
import gov.nist.secauto.metaschema.core.util.CollectionUtil
import gov.nist.secauto.metaschema.core.util.UriUtils
import gov.nist.secauto.metaschema.databind.DefaultBindingContext
import gov.nist.secauto.metaschema.databind.IBindingContext
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Paths

class QueryCommand : AbstractTerminalCommand() {
    companion object {

        private val LOGGER = LogManager.getLogger(QueryCommand::class.java)

        private const val COMMAND = "eval"
        val EXPRESSION_OPTION: Option = Option.builder("e")
            .longOpt("expression")
            .required()
            .hasArg()
            .argName("EXPRESSION")
            .desc("Metapath expression to execute")
            .build()

        val CONTENT_OPTION: Option = Option.builder("i")
            .hasArg()
            .argName("FILE_OR_URL")
            .desc("Metaschema content instance resource")
            .build()

        val OUTPUT_OPTION: Option = Option.builder("o")
            .hasArg()
            .argName("FILE_OR_URL")
            .desc("Output")
            .build()

        val METASCHEMA_OPTION: Option = Option.builder("m")
            .hasArg()
            .argName("FILE_OR_URL")
            .desc("metaschema resource")
            .build()
    }

    override fun getName(): String = COMMAND

    override fun getDescription(): String = "Execute a Metapath expression against a document"

    override fun gatherOptions(): Collection<Option> = listOf(
        METASCHEMA_OPTION,
        CONTENT_OPTION,
        EXPRESSION_OPTION,
        OUTPUT_OPTION
    )

    override fun getExtraArguments(): List<ExtraArgument> = CollectionUtil.emptyList()

    @Throws(InvalidArgumentException::class)
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
        return object : ICommandExecutor {
            override fun execute() {
                executeCommand(callingContext, cmdLine)
            }
        }
    }

    protected fun executeCommand(callingContext: CallingContext, cmdLine: CommandLine): ExitStatus {
        LOGGER.info("Starting execution of QueryCommand")
        val cwd = Paths.get("").toAbsolutePath().toUri()
        LOGGER.info("Current working directory: $cwd")

        val item: IItem? = when {
            cmdLine.hasOption(CONTENT_OPTION) -> {
                try {
                    val oscalBindingContext = OscalBindingContext.instance()
                    LOGGER.info("Created OSCAL binding context")
                    val module = oscalBindingContext.registerModule(OscalCompleteModule::class.java)
                    LOGGER.info("Registered OSCAL complete module")

                    if (cmdLine.hasOption(CONTENT_OPTION)) {
                        LOGGER.info("Content option detected")
                        val loader = oscalBindingContext.newBoundLoader()
                        LOGGER.info("Created bound loader")

                        val contentResource = try {
                            LOGGER.info("Attempting to handle content resource")
                            UriUtils.toUri(cmdLine.getOptionValue(CONTENT_OPTION), cwd);
                        } catch (ex: IOException) {
                            LOGGER.error("Failed to resolve content location", ex)
                            return ExitCode.INVALID_ARGUMENTS
                                .exitMessage("Unable to resolve content location. ${ex.message}")
                                .withThrowable(ex)
                        }

                        try {
                            LOGGER.info("Loading content as node item")
                            loader.loadAsNodeItem(contentResource as URI)
                        } catch (ex: IOException) {
                            LOGGER.error("Failed to load content", ex)
                            return ExitCode.INVALID_ARGUMENTS
                                .exitMessage("Unable to load content. ${ex.message}")
                                .withThrowable(ex)
                        }
                    } else {
                        LOGGER.info("No content option, creating new module node item")
                        INodeItemFactory.instance().newModuleNodeItem(module)
                    }
                } catch (ex: URISyntaxException) {
                    LOGGER.error("Invalid URI syntax", ex)
                    return ExitCode.INVALID_ARGUMENTS
                        .exitMessage("Cannot load module as '${ex.input}' is not a valid file or URL.")
                        .withThrowable(ex)
                } catch (ex: IOException) {
                    LOGGER.error("IO exception occurred", ex)
                    return ExitCode.PROCESSING_ERROR.exit().withThrowable(ex)
                } catch (ex: MetaschemaException) {
                    LOGGER.error("Metaschema exception occurred", ex)
                    return ExitCode.PROCESSING_ERROR.exit().withThrowable(ex)
                }
            }
            else -> {
                LOGGER.info("No Content option provided")
                null
            }
        }

        val expression = cmdLine.getOptionValue(EXPRESSION_OPTION)
        LOGGER.info("Expression to evaluate: $expression")

        return try {
            LOGGER.info("Compiling Metapath expression")
            val staticContext = OscalBindingContext.OSCAL_STATIC_METAPATH_CONTEXT
            val dynamicContext = DynamicContext(StaticContext.builder()
            .defaultModelNamespace(OscalModelConstants.NS_URI_OSCAL)
            .build());
            LOGGER.info("Compiling Metapath expression")
             val compiledMetapath: MetapathExpression = try {
                MetapathExpression.compile(expression, staticContext).also {
                    LOGGER.info("Metapath expression compiled successfully")
                }
            } catch (ex: Exception) {  // Replace with actual exception type
                LOGGER.error("Metapath expression did not compile", ex)
                return ExitCode.FAIL.exit()
            }

                    
            LOGGER.info("Evaluating compiled Metapath expression")
            val sequence: ISequence<INodeItem> = compiledMetapath.evaluate<INodeItem>(item, dynamicContext)
            LOGGER.info("Evaluation completed")

            val stringWriter = StringWriter()
            PrintWriter(stringWriter).use { writer ->
                LOGGER.info("Writing sequence to string")
                val itemWriter: IItemWriter = JsonItemWriter(writer,OscalBindingContext.instance())
                itemWriter.writeSequence(sequence)
            }

            LOGGER.info("Evaluation result:")
            LOGGER.info(stringWriter.toString())
            if (cmdLine.hasOption(OUTPUT_OPTION)) {
                val outputPath = Paths.get(cmdLine.getOptionValue(OUTPUT_OPTION))
                LOGGER.info("Writing output to file: $outputPath")
                Files.write(outputPath, stringWriter.toString().toByteArray())
                LOGGER.info("Output written to file successfully")
            }

            LOGGER.info("QueryCommand execution completed successfully")
            ExitCode.OK.exit()
        } catch (ex: Exception) {
            LOGGER.error("Error occurred during expression evaluation", ex)
            ExitCode.PROCESSING_ERROR.exit().withThrowable(ex)
        }
    }
}
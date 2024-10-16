/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server.core.commands

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
        EXPRESSION_OPTION
    )

    override fun getExtraArguments(): List<ExtraArgument> = CollectionUtil.emptyList()

    @Throws(InvalidArgumentException::class)
    override fun validateOptions(callingContext: CallingContext, cmdLine: CommandLine) {
        val extraArgs = cmdLine.argList
        if (extraArgs.isNotEmpty()) {
            throw InvalidArgumentException("Illegal number of extra arguments.")
        }
    }

    override fun newExecutor(callingContext: CallingContext, cmdLine: CommandLine): ICommandExecutor {
        return ICommandExecutor.using(callingContext, cmdLine) { ctx, cmd -> executeCommand(ctx, cmd) }
    }

    protected fun executeCommand(callingContext: CallingContext, cmdLine: CommandLine): ExitStatus {
        val cwd = Paths.get("").toAbsolutePath().toUri()

        val (module, item) = when {
            cmdLine.hasOption(METASCHEMA_OPTION) -> {
                try {
                    val moduleName = cmdLine.getOptionValue(METASCHEMA_OPTION)
                    val moduleUri = UriUtils.toUri(moduleName, cwd)
                    val module = MetaschemaCommands.handleModule(moduleUri, CollectionUtil.emptyList())

                    if (cmdLine.hasOption(CONTENT_OPTION)) {
                        // load the content
                        val bindingContext = DefaultBindingContext()

                        try {
                            val compilePath = Files.createTempDirectory("validation-")
                            compilePath.toFile().deleteOnExit()

                            bindingContext.registerModule(module, compilePath)
                        } catch (ex: IOException) {
                            return ExitCode.PROCESSING_ERROR
                                .exitMessage("Unable to get binding context. ${ex.message}")
                                .withThrowable(ex)
                        }

                        val loader = bindingContext.newBoundLoader()

                        val contentResource = try {
                            MetaschemaCommands.handleResource(cmdLine.getOptionValue(CONTENT_OPTION), cwd)
                        } catch (ex: IOException) {
                            return ExitCode.INVALID_ARGUMENTS
                                .exitMessage("Unable to resolve content location. ${ex.message}")
                                .withThrowable(ex)
                        }

                        try {
                            Pair(module, loader.loadAsNodeItem(contentResource))
                        } catch (ex: IOException) {
                            return ExitCode.INVALID_ARGUMENTS
                                .exitMessage("Unable to resolve content location. ${ex.message}")
                                .withThrowable(ex)
                        }
                    } else {
                        Pair(module, INodeItemFactory.instance().newModuleNodeItem(module))
                    }
                } catch (ex: URISyntaxException) {
                    return ExitCode.INVALID_ARGUMENTS
                        .exitMessage("Cannot load module as '${ex.input}' is not a valid file or URL.")
                        .withThrowable(ex)
                } catch (ex: IOException) {
                    return ExitCode.PROCESSING_ERROR.exit().withThrowable(ex)
                } catch (ex: MetaschemaException) {
                    return ExitCode.PROCESSING_ERROR.exit().withThrowable(ex)
                }
            }
            cmdLine.hasOption(CONTENT_OPTION) -> {
                return ExitCode.INVALID_ARGUMENTS.exitMessage(
                    "Must use '${CONTENT_OPTION.argName}' to specify the Metaschema module."
                )
            }
            else -> Pair(null, null)
        }

        val expression = cmdLine.getOptionValue(EXPRESSION_OPTION)

        val staticContext = StaticContext.builder().apply {
            module?.let { defaultModelNamespace(it.xmlNamespace) }
        }.build()

        return try {
            // Parse and compile the Metapath expression
            val compiledMetapath: MetapathExpression = MetapathExpression.compile(expression, staticContext)
                    
            // Use a wildcard type for the sequence
            val sequence: ISequence<INodeItem> = compiledMetapath.evaluate<INodeItem>(item, DynamicContext(staticContext))

            val stringWriter = StringWriter()
            PrintWriter(stringWriter).use { writer ->
                val itemWriter: IItemWriter = DefaultItemWriter(writer)
                itemWriter.writeSequence(sequence)
            }

            // Print the result
            if (LOGGER.isInfoEnabled) {
                LOGGER.info(stringWriter.toString())
            }

            ExitCode.OK.exit()
        } catch (ex: Exception) {
            ExitCode.PROCESSING_ERROR.exit().withThrowable(ex)
        }
    }
}
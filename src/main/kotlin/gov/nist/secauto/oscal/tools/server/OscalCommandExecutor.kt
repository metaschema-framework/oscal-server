/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server.commands

import gov.nist.secauto.metaschema.cli.processor.CLIProcessor
import gov.nist.secauto.metaschema.cli.processor.command.ICommand
import gov.nist.secauto.metaschema.cli.processor.command.ICommandExecutor
import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import gov.nist.secauto.metaschema.cli.processor.ExitCode
import gov.nist.secauto.metaschema.cli.processor.InvalidArgumentException
import gov.nist.secauto.metaschema.databind.IBindingContext
import gov.nist.secauto.oscal.lib.OscalBindingContext
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.util.LinkedList
import java.util.Deque
import gov.nist.secauto.oscal.tools.cli.core.commands.ConvertCommand
import gov.nist.secauto.oscal.tools.cli.core.commands.ValidateCommand
import gov.nist.secauto.oscal.tools.cli.core.commands.ResolveCommand

open class OscalCommandExecutor(
    protected val command: String,
    protected val args: List<String>,
) : CLIProcessor("oscal-server"), ICommandExecutor {

    protected val logger: Logger = LogManager.getLogger(this::class.java)
    protected open val commands: Map<String, () -> ICommand> = mapOf(
        "validate" to ::ValidateCommand,
        "convert" to ::ConvertCommand,
        "resolve-profile" to ::ResolveCommand
    )

    protected open fun getBindingContext(): IBindingContext {
        return OscalBindingContext.instance()
    }

    override fun execute(): ExitStatus {
        logger.info("Executing command: $command")
        logger.info("With args: $args")

        val callingContext = createCallingContext(command, args)
        return callingContext.processCommand()
    }

    protected open fun createCallingContext(command: String, args: List<String>): OscalCallingContext {
        logger.debug("Creating call context")

        return OscalCallingContext(command, args)
    }

    open inner class OscalCallingContext(val command: String, args: List<String>) : CallingContext(args) {
        protected val oscalOptions: List<org.apache.commons.cli.Option>
        protected val oscalCalledCommands: Deque<ICommand> = LinkedList()
        protected val oscalExtraArgs: List<String>
        init {
            logger.debug("$command context initializing");
            val topLevelCommandMap = commands.mapValues { (_, factory) -> factory() }

            val tempOptions = LinkedList(OPTIONS)
            val tempExtraArgs = LinkedList<String>()

            var endArgs = false
            for (arg in args) {
                when {
                    endArgs || arg.startsWith("-") -> tempExtraArgs.add(arg)
                    arg == "--" -> endArgs = true
                    else -> {
                        val cmd = if (oscalCalledCommands.isEmpty()) {
                            topLevelCommandMap[arg]
                        } else {
                            oscalCalledCommands.last.getSubCommandByName(arg)
                        }

                        if (cmd == null) {
                            tempExtraArgs.add(arg)
                            endArgs = true
                        } else {
                            oscalCalledCommands.add(cmd)
                        }
                    }
                }
            }

            for (cmd in oscalCalledCommands) {
                tempOptions.addAll(cmd.gatherOptions())
            }

            oscalOptions = tempOptions.toList()
            oscalExtraArgs = tempExtraArgs.toList()
        }

        override fun processCommand(): ExitStatus {
            val parser: CommandLineParser = DefaultParser()
            lateinit var cmdLine: CommandLine

            // Phase 1: Check for help or version
            run {
                try {
                    val phase1Options = Options().apply {
                        addOption(HELP_OPTION)
                        addOption(VERSION_OPTION)
                    }
                    cmdLine = parser.parse(phase1Options, oscalExtraArgs.toTypedArray(), true)
                } catch (ex: ParseException) {
                    return handleInvalidCommand(ex.message ?: "Invalid command")
                }

                if (cmdLine.hasOption(VERSION_OPTION)) {
                    showVersion()
                    return ExitCode.OK.exit()
                } else if (cmdLine.hasOption(HELP_OPTION)) {
                    showHelp()
                    return ExitCode.OK.exit()
                }
            }

            // Phase 2: Execute the command
            try {
                cmdLine = parser.parse(toOptions(), oscalExtraArgs.toTypedArray())
            } catch (ex: ParseException) {
                return handleInvalidCommand(ex.message ?: "Invalid command")
            }

            return invokeCommand(cmdLine)
        }

        override protected open fun invokeCommand(cmdLine: CommandLine): ExitStatus {
            return try {
                for (cmd in oscalCalledCommands) {
                    try {
                        cmd.validateOptions(this, cmdLine)
                    } catch (ex: InvalidArgumentException) {
                        return handleInvalidCommand(ex.message ?: "Invalid argument")
                    }
                }

                val targetCommand = oscalCalledCommands.lastOrNull()
                if (targetCommand == null) {
                    ExitCode.INVALID_COMMAND.exit()
                } else {
                    val executor = targetCommand.newExecutor(this, cmdLine)
                    executor.execute()
                }
            } catch (ex: RuntimeException) {
                ExitCode.RUNTIME_ERROR
                    .exitMessage("An uncaught runtime error occurred. ${ex.localizedMessage}")
                    .withThrowable(ex)
            }
        }

        override fun handleInvalidCommand(message: String): ExitStatus {
            val status = ExitCode.INVALID_COMMAND.exitMessage(message)
            return status
        }


        override fun toOptions(): Options {
            return Options().apply {
                oscalOptions.forEach { addOption(it) }
            }
        }
    }
}
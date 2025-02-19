package gov.nist.secauto.oscal.tools.server

import gov.nist.secauto.metaschema.cli.processor.ExitCode
import gov.nist.secauto.metaschema.cli.processor.command.CommandExecutionException
import gov.nist.secauto.metaschema.databind.io.Format
import gov.nist.secauto.metaschema.databind.io.IBoundLoader
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.util.Locale
import java.util.Arrays

/**
 * Utility class for detecting and determining source formats for content files.
 */
class FormatDetector {
    companion object {
        /**
         * Detect the source format for content identified using the provided option.
         *
         * This method will first check if the source format is explicitly declared on
         * the command line. If so, this format will be returned.
         *
         * If not, then the content will be analyzed to determine the format.
         *
         * @param commandLine the provided command line argument information
         * @param option the option specifying the format, which must be present on the command line
         * @param loader the content loader to use to load the content instance
         * @param resource the resource to load
         * @return the identified content format
         * @throws CommandExecutionException if an error occurred while determining the source format
         */
        @JvmStatic
        fun determineSourceFormat(
            commandLine: CommandLine,
            option: Option,
            loader: IBoundLoader,
            resource: URI
        ): Format {
            if (commandLine.hasOption(option)) {
                // use the option
                return getFormat(commandLine, option)
            }

            // attempt to determine the format
            try {
                return loader.detectFormat(resource)
            } catch (ex: FileNotFoundException) {
                // this case was already checked for
                throw CommandExecutionException(
                    ExitCode.IO_ERROR,
                    "The provided source '${resource}' does not exist.",
                    ex
                )
            } catch (ex: IOException) {
                throw CommandExecutionException(
                    ExitCode.IO_ERROR,
                    "Unable to determine source format. Use '${if (option.hasLongOpt()) "--${option.longOpt}" else "-${option.opt}"}' to specify the format. ${ex.localizedMessage}",
                    ex
                )
            }
        }

        /**
         * Parse the command line options to get the selected format.
         *
         * @param commandLine the provided command line argument information
         * @param option the option specifying the format, which must be present on the command line
         * @return the format
         * @throws CommandExecutionException if the format option was not provided or was an invalid choice
         */
        @JvmStatic
        private fun getFormat(
            commandLine: CommandLine,
            option: Option
        ): Format {
            // use the option
            val toFormatText = commandLine.getOptionValue(option) ?: throw CommandExecutionException(
                ExitCode.INVALID_ARGUMENTS,
                "The '${if (option.hasLongOpt()) "--${option.longOpt}" else "-${option.opt}"}' argument was not provided."
            )

            try {
                return Format.valueOf(toFormatText.uppercase(Locale.ROOT))
            } catch (ex: IllegalArgumentException) {
                throw CommandExecutionException(
                    ExitCode.INVALID_ARGUMENTS,
                    "Invalid '${if (option.hasLongOpt()) "--${option.longOpt}" else "-${option.opt}"}' argument. The format must be one of: ${
                        Arrays.stream(Format.values())
                            .map { it.name }
                            .collect(CustomCollectors.joiningWithOxfordComma("or"))
                    }."
                )
            }
        }
    }
}

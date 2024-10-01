package gov.nist.secauto.oscal.tools.server.commands
import gov.nist.secauto.metaschema.cli.processor.CLIProcessor.CallingContext
import gov.nist.secauto.metaschema.cli.processor.command.ICommandExecutor
import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import gov.nist.secauto.metaschema.cli.processor.MessageExitStatus
import gov.nist.secauto.metaschema.cli.processor.ExitCode
import gov.nist.secauto.metaschema.core.model.IBoundObject
import gov.nist.secauto.metaschema.databind.IBindingContext
import gov.nist.secauto.metaschema.databind.io.Format
import gov.nist.secauto.metaschema.databind.io.FormatDetector
import gov.nist.secauto.metaschema.databind.io.IBoundLoader
import gov.nist.secauto.metaschema.databind.io.ISerializer
import gov.nist.secauto.metaschema.databind.io.ModelDetector
import gov.nist.secauto.oscal.lib.OscalBindingContext
import org.apache.commons.cli.CommandLine
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.Writer
import java.net.URI

class OscalCommandExecutor(
    private val args: List<String>,
    private val outputStream: ByteArrayOutputStream,
    private val errorStream: ByteArrayOutputStream,
    private val fileStream: ByteArrayOutputStream
) : ICommandExecutor {
    private fun getBindingContext(): IBindingContext {
        return OscalBindingContext.instance()
    }

    @Throws(FileNotFoundException::class, IOException::class)
    private fun handleConversion(source: URI, toFormat: Format, writer: Writer, loader: IBoundLoader) {
        var boundClass: Class<out IBoundObject>
        var boundObject: IBoundObject

        source.toURL().openStream().use { inputStream ->
            val formatResult = loader.detectFormat(inputStream)
            val sourceFormat = formatResult.format

            formatResult.dataStream.use { fis ->
                loader.detectModel(fis, sourceFormat).use { modelResult ->
                    boundClass = modelResult.boundClass
                    modelResult.dataStream.use { mis ->
                        boundObject = loader.load(boundClass, sourceFormat, mis, source)
                    }
                }
            }
        }

        val serializer: ISerializer<*> = getBindingContext().newSerializer(toFormat, boundClass)
        serializer.serialize(boundObject, writer)
    }

    override fun execute(): ExitStatus {
        if (args.size < 2) {
            errorStream.write("Error: Insufficient arguments\n".toByteArray())
            return MessageExitStatus(ExitCode.INVALID_ARGUMENTS)
        }

        val sourcePath = args[0]
        val targetFormat = args[1]

        try {
            val sourceUri = URI(sourcePath)
            val toFormat = Format.valueOf(targetFormat.uppercase())
            val bindingContext = getBindingContext()
            val loader = bindingContext.newBoundLoader()

            val writer = outputStream.writer()
            handleConversion(sourceUri, toFormat, writer, loader)
            writer.flush()

            return MessageExitStatus(ExitCode.OK)
        } catch (e: IllegalArgumentException) {
            errorStream.write("Error: Invalid format specified\n".toByteArray())
            return MessageExitStatus(ExitCode.INVALID_ARGUMENTS)
        } catch (e: FileNotFoundException) {
            errorStream.write("Error: Source file not found\n".toByteArray())
            return MessageExitStatus(ExitCode.IO_ERROR)
        } catch (e: IOException) {
            errorStream.write("Error: IO Exception occurred\n".toByteArray())
            e.printStackTrace(PrintStream(errorStream))
            return MessageExitStatus(ExitCode.IO_ERROR)
        } catch (e: Exception) {
            errorStream.write("Error: Unexpected exception occurred\n".toByteArray())
            e.printStackTrace(PrintStream(errorStream))
            return MessageExitStatus(ExitCode.PROCESSING_ERROR)
        }
    }
}
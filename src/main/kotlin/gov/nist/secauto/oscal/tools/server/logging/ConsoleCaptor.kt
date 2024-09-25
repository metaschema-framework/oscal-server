
package gov.nist.secauto.oscal.tools.server.logging

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream
import java.io.UncheckedIOException
class ConsoleCaptor private constructor(
    private val allowEmptyLines: Boolean,
    private val allowTrimmingWhiteSpace: Boolean
) : AutoCloseable {

    private lateinit var outputStreamForOut: ByteArrayOutputStream
    private lateinit var outputStreamForErr: ByteArrayOutputStream
    private lateinit var consoleCaptorForOut: PrintStream
    private lateinit var consoleCaptorForErr: PrintStream

    init {
        createStreams()
        insertStreamsToSystemOut()
    }

    private fun createStreams() {
        outputStreamForOut = ByteArrayOutputStream()
        outputStreamForErr = ByteArrayOutputStream()

        consoleCaptorForOut = PrintStream(outputStreamForOut)
        consoleCaptorForErr = PrintStream(outputStreamForErr)
    }

    private fun insertStreamsToSystemOut() {
        System.setOut(consoleCaptorForOut)
        System.setErr(consoleCaptorForErr)
    }

    val standardOutput: List<String>
        get() = getContent(outputStreamForOut)

    val errorOutput: List<String>
        get() = getContent(outputStreamForErr)

    private fun getContent(outputStream: ByteArrayOutputStream): List<String> {
        return outputStream.toString().split(System.lineSeparator())
            .map { if (allowTrimmingWhiteSpace) it.trim() else it }
            .filter { allowEmptyLines || it.isNotEmpty() }
    }

    fun clearOutput() {
        closeExistingStreams()
        createStreams()
        insertStreamsToSystemOut()
    }

    private fun closeExistingStreams() {
        try {
            outputStreamForOut.flush()
            outputStreamForErr.flush()
            consoleCaptorForOut.flush()
            consoleCaptorForErr.flush()

            outputStreamForOut.close()
            outputStreamForErr.close()

            consoleCaptorForOut.close()
            consoleCaptorForErr.close()
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    override fun close() {
        rollBackConfiguration()
        closeExistingStreams()
    }

    private fun rollBackConfiguration() {
        System.setOut(originalOut)
        System.setErr(originalErr)
    }

    companion object {
        private val originalOut: PrintStream = System.out
        private val originalErr: PrintStream = System.err

        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    class Builder {
        private var allowEmptyLines = false
        private var allowTrimmingWhiteSpace = true

        fun allowEmptyLines(allowEmptyLines: Boolean) = apply { this.allowEmptyLines = allowEmptyLines }

        fun allowTrimmingWhiteSpace(allowTrimmingWhiteSpace: Boolean) = apply { this.allowTrimmingWhiteSpace = allowTrimmingWhiteSpace }

        fun build() = ConsoleCaptor(allowEmptyLines, allowTrimmingWhiteSpace)
    }
}

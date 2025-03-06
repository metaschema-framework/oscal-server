package gov.nist.secauto.oscal.tools.server

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager

class UrlProcessor(private val allowedDirs: List<Path>) {
    private val logger: Logger = LogManager.getLogger(UrlProcessor::class.java)

    fun processUrl(url: String): String {
        return when {
            url.startsWith("https://") -> url
            url.startsWith("file://") -> processFileUrl(url)
            isLocalPath(url) -> processFileUrl("file://$url")
            else -> {
                logger.error("Invalid URL scheme: $url")
                throw SecurityException("Only https:// URLs or allowed local files are permitted.")
            }
        }
    }

    private fun isLocalPath(path: String): Boolean {
        return path.matches("""^[a-zA-Z]:[/\\].*|/.*|~.*|\w+[/\\].*$""".toRegex())
    }

    private fun processFileUrl(url: String): String {
        try {
            val filePrefix = "file://"
            val decodedPath = URLDecoder.decode(
                if (url.startsWith(filePrefix)) url.substring(filePrefix.length) else url,
                StandardCharsets.UTF_8.name()
            )
            
            val expandedPath = if (decodedPath.startsWith("~")) {
                System.getProperty("user.home") + decodedPath.substring(1)
            } else {
                decodedPath
            }

            val normalizedPath = if (System.getProperty("os.name").lowercase().contains("win")) {
                val winPath = if (expandedPath.startsWith("/")) {
                    expandedPath.substring(1).replace('/', '\\')
                } else {
                    expandedPath.replace('/', '\\')
                }
                Paths.get(winPath).normalize().toAbsolutePath()
            } else {
                Paths.get(expandedPath).normalize().toAbsolutePath()
            }

            val canonicalPath = normalizedPath.toFile().canonicalPath
            if (canonicalPath != normalizedPath.toString()) {
                throw SecurityException("Potential directory traversal detected")
            }

            if (!allowedDirs.any { allowedDir -> normalizedPath.startsWith(allowedDir) }) {
                throw SecurityException("Access denied: File is not in an allowed directory: $normalizedPath")
            }

            return normalizedPath.toString()
        } catch (e: Exception) {
            logger.error("Error processing file URL: $url", e)
            throw SecurityException("Invalid file URL: ${e.message}")
        }
    }
}
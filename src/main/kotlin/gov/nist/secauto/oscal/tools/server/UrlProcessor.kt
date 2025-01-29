/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager

class UrlProcessor(private val allowedDirs: List<Path>) {
    private val logger: Logger = LogManager.getLogger(UrlProcessor::class.java)

    fun processUrl(url: String): String {
        return when {
            url.startsWith("https://") -> {
                // HTTPS URLs are allowed as-is
                url
            }
            url.startsWith("file://") -> {
                processFileUrl(url)
            }
            else -> {
                logger.error("Invalid URL scheme: $url")
                throw SecurityException("Only https:// URLs or allowed local files are permitted.")
            }
        }
    }

    private fun processFileUrl(url: String): String {
        try {
            val decodedPath = URLDecoder.decode(url.substring(7), StandardCharsets.UTF_8.name())
            val normalizedPath = if (System.getProperty("os.name").lowercase().contains("win")) {
                // Windows-specific handling
                val winPath = if (decodedPath.startsWith("/")) {
                    decodedPath.substring(1).replace('/', '\\')
                } else {
                    decodedPath.replace('/', '\\')
                }
                Paths.get(winPath).normalize().toAbsolutePath()
            } else {
                Paths.get(decodedPath).normalize().toAbsolutePath()
            }

            // Check for directory traversal attempts
            val canonicalPath = normalizedPath.toFile().canonicalPath
            if (canonicalPath != normalizedPath.toString()) {
                throw SecurityException("Potential directory traversal detected")
            }

            // Verify path is under allowed directories
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

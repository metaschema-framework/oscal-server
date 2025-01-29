/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import java.nio.file.attribute.PosixFilePermission
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.io.File
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager

class DirectoryManager {
    private val logger: Logger = LogManager.getLogger(DirectoryManager::class.java)
    private lateinit var oscalDir: Path
    private val allowedDirs = mutableListOf<Path>()

    fun initialize(): Path {
        validateAndInitializeDirectories()
        return oscalDir
    }

    fun getAllowedDirs(): List<Path> {
        return allowedDirs.toList()
    }

    private fun validateAndInitializeDirectories() {
        // Initialize OSCAL directory with security checks
        initializeOscalDirectory()
        
        // Initialize and validate allowed directories
        initializeAllowedDirectories()
        
        // Verify all directories are accessible and have proper permissions
        verifyDirectoryPermissions()
    }

    private fun initializeOscalDirectory() {
        val homeDir = System.getProperty("user.home")
        oscalDir = Paths.get(homeDir, ".oscal").normalize().toAbsolutePath()
        
        if (!Files.exists(oscalDir)) {
            try {
                // Create directory with restricted permissions (owner read/write/execute only)
                Files.createDirectory(oscalDir)
                restrictDirectoryPermissions(oscalDir)
            } catch (e: Exception) {
                throw SecurityException("Failed to create secure OSCAL directory", e)
            }
        }
        
        if (!Files.isDirectory(oscalDir)) {
            throw SecurityException("OSCAL path exists but is not a directory: $oscalDir")
        }
        
        logger.info("OSCAL directory initialized at: $oscalDir")
    }

    private fun initializeAllowedDirectories() {
        // Clear existing allowed directories
        allowedDirs.clear()
        
        // Always add ~/.oscal as the first allowed directory
        allowedDirs.add(oscalDir)

        val envPath = System.getenv("OSCAL_SERVER_PATH")
        if (!envPath.isNullOrBlank()) {
            val paths = envPath.split(File.pathSeparator)
            for (dir in paths) {
                try {
                    val expandedDir = expandHomeDirectory(dir.trim())
                    val path = Paths.get(expandedDir).normalize().toAbsolutePath()
                    
                    // Validate the directory
                    validateDirectory(path)
                    
                    // Add to allowed directories if validation passes
                    allowedDirs.add(path)
                    logger.info("Added allowed directory from OSCAL_SERVER_PATH: $path")
                } catch (e: Exception) {
                    logger.error("Invalid directory in OSCAL_SERVER_PATH: $dir", e)
                    throw SecurityException("Invalid directory configuration: $dir", e)
                }
            }
        } else {
            logger.warn("OSCAL_SERVER_PATH environment variable not set - only ~/.oscal will be accessible")
        }

        if (allowedDirs.isEmpty()) {
            throw SecurityException("No valid directories configured for access")
        }

        logger.info("Initialized allowed directories: ${allowedDirs.joinToString(", ")}")
    }

    private fun validateDirectory(path: Path) {
        when {
            !Files.exists(path) -> 
                throw SecurityException("Directory does not exist: $path")
            !Files.isDirectory(path) -> 
                throw SecurityException("Path is not a directory: $path")
            !Files.isReadable(path) -> 
                throw SecurityException("Directory is not readable: $path")
            path.startsWith(oscalDir) && !path.equals(oscalDir) -> 
                throw SecurityException("Security violation: Subdirectories of ~/.oscal are not allowed")
        }
    }

    private fun verifyDirectoryPermissions() {
        allowedDirs.forEach { dir ->
            try {
                // Verify basic access permissions
                require(Files.isReadable(dir)) { "Directory not readable: $dir" }
                require(Files.isExecutable(dir)) { "Directory not executable: $dir" }
                
                // Check for suspicious symlinks
                if (Files.isSymbolicLink(dir)) {
                    val target = Files.readSymbolicLink(dir)
                    val resolvedTarget = dir.resolveSibling(target).normalize()
                    require(allowedDirs.any { allowed -> resolvedTarget.startsWith(allowed) }) {
                        "Symbolic link points outside allowed directories: $dir -> $resolvedTarget"
                    }
                }
            } catch (e: Exception) {
                throw SecurityException("Directory permission verification failed for $dir: ${e.message}")
            }
        }
    }

    private fun restrictDirectoryPermissions(path: Path) {
        try {
            // Set directory permissions to owner read/write/execute only
            val perms = Files.getPosixFilePermissions(path)
            perms.removeAll(setOf(
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_WRITE,
                PosixFilePermission.OTHERS_EXECUTE
            ))
            Files.setPosixFilePermissions(path, perms)
        } catch (e: Exception) {
            logger.warn("Failed to restrict directory permissions: ${e.message}")
            // Continue execution but log the warning
        }
    }

    private fun expandHomeDirectory(path: String): String {
        return if (path.startsWith("~")) {
            val home = System.getProperty("user.home")
            home + path.substring(1)
        } else {
            path
        }
    }
}

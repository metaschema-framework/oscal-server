/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import gov.nist.secauto.metaschema.cli.processor.ExitCode
import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import gov.nist.secauto.metaschema.cli.processor.MessageExitStatus
import gov.nist.secauto.metaschema.databind.io.Format
import gov.nist.secauto.metaschema.databind.io.IBoundLoader
import gov.nist.secauto.metaschema.core.model.IBoundObject
import gov.nist.secauto.oscal.lib.OscalBindingContext
import gov.nist.secauto.oscal.lib.model.Profile
import gov.nist.secauto.oscal.lib.profile.resolver.ProfileResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Files
import java.nio.file.Path

class ProfileResolverService {
    private val logger: Logger = LogManager.getLogger(ProfileResolverService::class.java)
    private val context = OscalBindingContext.instance()

    suspend fun resolveProfile(inputPath: Path, outputPath: Path, format: String): Pair<ExitStatus, String> {
        logger.info("Resolving profile from $inputPath to $outputPath with format $format")
        return try {
            withContext(Dispatchers.IO) {
                val exitStatus = try {
                    val loader: IBoundLoader = context.newBoundLoader()
                    logger.info("Starting profile resolution")
                    
                    // Open input stream and detect format
                    logger.debug("Detecting source format...")
                    val inputStream = inputPath.toUri().toURL().openStream()
                    val formatResult = loader.detectFormat(inputStream, inputPath.toUri())
                    val sourceFormat = formatResult.getFormat()
                    logger.info("Detected source format: $sourceFormat")
                    
                    // Get data stream for loading
                    val dataStream = formatResult.getDataStream()
                    
                    // Load and validate profile
                    logger.debug("Loading and validating profile...")
                    val modelResult = loader.detectModel(dataStream, inputPath.toUri(), sourceFormat)
                    val modelInputStream = modelResult.getDataStream()
                    val document = loader.load(modelResult.getBoundClass(), sourceFormat, modelInputStream, inputPath.toUri())
                    
                    // Ensure the loaded document is a Profile
                    if (document !is Profile) {
                        logger.error("Document is not a Profile: ${document.javaClass.simpleName}")
                        throw IllegalArgumentException("Document is not a Profile")
                    }
                    logger.info("Profile loaded successfully")
                    
                    // Create temporary file for the profile
                    logger.debug("Creating temporary file for profile...")
                    val tempFile = Files.createTempFile(null, ".json")
                    try {
                        // Write the profile to temp file
                        Files.newOutputStream(tempFile).use { out ->
                            context.newSerializer(sourceFormat, Profile::class.java).serialize(document, out)
                        }
                        
                        // Create resolver and resolve profile
                        logger.debug("Creating profile resolver...")
                        val resolver = ProfileResolver()
                        logger.debug("Resolving profile...")
                        val resolvedProfile = resolver.resolve(tempFile) as IBoundObject
                        
                        // Serialize to the output
                        logger.debug("Serializing resolved profile...")
                        Files.newOutputStream(outputPath).use { out ->
                            val outputFormat = if (format.equals("json", true)) Format.JSON else Format.XML
                            context.newSerializer(outputFormat, resolvedProfile.javaClass).serialize(resolvedProfile, out)
                        }
                        logger.info("Profile resolution completed successfully")
                    } finally {
                        // Clean up temp file
                        Files.deleteIfExists(tempFile)
                    }
                    
                    MessageExitStatus(ExitCode.OK, "Profile resolution completed successfully")
                } catch (e: Exception) {
                    logger.error("Profile resolution failed", e)
                    MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message)
                }
                Pair(exitStatus, "")
            }
        } catch (e: Exception) {
            logger.error("Profile resolution failed with exception", e)
            MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message) to ""
        }
    }
}

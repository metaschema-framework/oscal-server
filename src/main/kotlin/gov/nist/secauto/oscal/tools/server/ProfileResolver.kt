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
 
     fun resolveProfile(inputPath: Path, outputPath: Path, format: String): Pair<ExitStatus, String> {
         logger.info("Resolving profile from $inputPath to $outputPath with format $format")
         return try {
                 val exitStatus = try {
                     val loader: IBoundLoader = context.newBoundLoader()
                     logger.info("Starting profile resolution")
                     
                     // Validate format parameter
                     val outputFormat = when (format.lowercase()) {
                         "json" -> Format.JSON
                         "xml" -> Format.XML
                         else -> throw IllegalArgumentException("Unsupported format: $format. Must be 'json' or 'xml'")
                     }
                     
                     // Load and validate profile
                     logger.debug("Loading and validating profile...")
                     val document = inputPath.toUri().toURL().openStream().use { inputStream ->
                         val formatResult = loader.detectFormat(inputStream, inputPath.toUri())
                         val sourceFormat = formatResult.getFormat()
                         logger.info("Detected source format: $sourceFormat")
                         
                         val modelResult = loader.detectModel(formatResult.getDataStream(), inputPath.toUri(), sourceFormat)
                         loader.load(modelResult.getBoundClass(), sourceFormat, modelResult.getDataStream(), inputPath.toUri())
                     }
                     
                     // Ensure the loaded document is a Profile
                     if (document !is Profile) {
                         val msg = "Document is not a Profile: ${document.javaClass.simpleName}"
                         logger.error(msg)
                         throw IllegalArgumentException(msg)
                     }
                     logger.info("Profile loaded successfully")
                     
                     // Create resolver and resolve profile
                     logger.debug("Creating profile resolver...")
                     val resolver = ProfileResolver()
                     
                     // Log the parent directory to help with debugging
                     val parentDir = inputPath.parent
                     logger.info("Looking for catalog files in: $parentDir")
                     
                     // List files in the parent directory to help with debugging
                     if (Files.exists(parentDir)) {
                         Files.list(parentDir).forEach { file ->
                             logger.info("Found file in parent directory: $file")
                         }
                     }
                     
                     logger.info("Resolving profile...")
                     try {
                         val resolvedDocument = resolver.resolve(inputPath)
                         logger.info("Profile resolved successfully")
                         
                         // Get the catalog from the document
                         val catalogRoot = resolvedDocument.getModelItemsByName("catalog").firstOrNull()
                         if (catalogRoot == null) {
                             throw IllegalStateException("Resolved document does not contain a catalog")
                         }
                         
                         val resolvedCatalog = catalogRoot.value as IBoundObject
                         
                         // Serialize resolved profile to the output
                         logger.info("Serializing resolved profile...")
                         Files.newOutputStream(outputPath).use { out ->
                             context.newSerializer(outputFormat, resolvedCatalog.javaClass).serialize(resolvedCatalog, out)
                         }
                         logger.info("Profile resolution completed successfully")
                         
                         MessageExitStatus(ExitCode.OK, "Profile resolution completed successfully")
                     } catch (e: Exception) {
                         logger.error("Error resolving profile: ${e.message}", e)
                         e.printStackTrace()
                         throw e
                     }
                     
                 } catch (e: Exception) {
                     val errorMsg = when (e) {
                         is IllegalArgumentException -> e.message ?: "Invalid input"
                         is java.nio.file.NoSuchFileException -> "Input file not found: ${e.file}"
                         is java.io.IOException -> "I/O error: ${e.message}"
                         else -> "Profile resolution failed: ${e.message}"
                     }
                     logger.error(errorMsg, e)
                     MessageExitStatus(ExitCode.RUNTIME_ERROR, errorMsg)
                 }
                 Pair(exitStatus, "")
             
         } catch (e: Exception) {
             val errorMsg = "Unexpected error during profile resolution: ${e.message}"
             logger.error(errorMsg, e)
             MessageExitStatus(ExitCode.RUNTIME_ERROR, errorMsg) to ""
         }
     }
 }

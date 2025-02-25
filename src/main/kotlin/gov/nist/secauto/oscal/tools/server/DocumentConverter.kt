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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Files
import java.nio.file.Path

class DocumentConverter {
    private val logger: Logger = LogManager.getLogger(DocumentConverter::class.java)
    private val context = OscalBindingContext.instance()

    suspend fun convertDocument(inputPath: Path, outputPath: Path, format: String): Pair<ExitStatus, String> {
        logger.info("Converting document from $inputPath to $outputPath with format $format")
        return try {
                val exitStatus = try {
                    val loader: IBoundLoader = context.newBoundLoader()
                    logger.info("Starting document conversion")
                    val outputFormat = if (format.equals("json", true)) Format.JSON else Format.XML
                    logger.debug("Target output format: $outputFormat")
                    
                    // Open input stream and detect format
                    logger.debug("Detecting source format...")
                    val inputStream = inputPath.toUri().toURL().openStream()
                    val formatResult = loader.detectFormat(inputStream, inputPath.toUri())
                    val sourceFormat = formatResult.getFormat()
                    logger.info("Detected source format: $sourceFormat")
                    
                    // Get data stream for loading
                    val dataStream = formatResult.getDataStream()
                    
                    // Detect and load the document
                    logger.debug("Loading document and detecting type...")
                    val modelResult = loader.detectModel(dataStream, inputPath.toUri(), sourceFormat)
                    val boundClass = modelResult.getBoundClass()
                    val modelInputStream = modelResult.getDataStream()
                    val document = loader.load(boundClass, sourceFormat, modelInputStream, inputPath.toUri())
                    logger.info("Document loaded as: ${boundClass.simpleName}")
                    
                    // Serialize to output format
                    logger.debug("Serializing to output format...")
                    Files.newOutputStream(outputPath).use { out ->
                        context.newSerializer(outputFormat, boundClass).serialize(document, out)
                    }
                    logger.info("Document successfully converted from $sourceFormat to $outputFormat")
                    
                    MessageExitStatus(ExitCode.OK, "Conversion completed successfully")
                } catch (e: Exception) {
                    logger.error("Conversion failed", e)
                    MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message)
                }
                Pair(exitStatus, "")
            
        } catch (e: Exception) {
            logger.error("Conversion failed with exception", e)
            MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message) to ""
        }
    }
}

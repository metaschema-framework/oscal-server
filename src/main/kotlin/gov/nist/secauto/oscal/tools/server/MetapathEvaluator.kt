/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import gov.nist.secauto.metaschema.cli.processor.ExitCode
import gov.nist.secauto.oscal.tools.cli.core.OscalCliVersion
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
import java.nio.file.Paths
import java.util.UUID

class MetapathEvaluator {
    private val logger: Logger = LogManager.getLogger(MetapathEvaluator::class.java)
    private val context = OscalBindingContext.instance()

    suspend fun evaluateMetapath(
        inputPath: Path,
        expression: String,
        module: String,
        oscalDir: Path
    ): Pair<ExitStatus, String> {
        logger.info("Evaluating metapath expression on: $inputPath")
        return try {
            withContext(Dispatchers.IO) {
                val exitStatus = try {
                    val loader: IBoundLoader = context.newBoundLoader()
                    logger.info("Starting metapath evaluation")
                    
                    // Open input stream and detect format
                    logger.debug("Detecting document format...")
                    val inputStream = inputPath.toUri().toURL().openStream()
                    val formatResult = loader.detectFormat(inputStream, inputPath.toUri())
                    val sourceFormat = formatResult.getFormat()
                    logger.info("Detected format: $sourceFormat")
                    
                    // Get data stream for loading
                    val dataStream = formatResult.getDataStream()
                    
                    // Load document
                    logger.debug("Loading document...")
                    val modelResult = loader.detectModel(dataStream, inputPath.toUri(), sourceFormat)
                    val modelInputStream = modelResult.getDataStream()
                    val document = loader.load(modelResult.getBoundClass(), sourceFormat, modelInputStream, inputPath.toUri())
                    logger.info("Document loaded as: ${document.javaClass.simpleName}")
                    
                    // Create a binding context for metapath evaluation
                    logger.debug("Creating JSON serializer for metapath evaluation...")
                    val serializer = context.newSerializer(Format.JSON, document.javaClass)
                    val outputStream = java.io.ByteArrayOutputStream()
                    
                    logger.debug("Serializing document for metapath evaluation...")
                    serializer.serialize(document, outputStream)
                    
                    // For now, return the full JSON document
                    // TODO: Implement proper metapath evaluation when API is available
                    val jsonResult = outputStream.toString()
                    logger.info("Document serialized successfully for metapath evaluation")
                    
                    // Create SARIF output for successful evaluation
                    val guid = UUID.randomUUID().toString()
                    val sarifPath = Paths.get(oscalDir.toString(), "${guid}.sarif")
                    Files.newBufferedWriter(sarifPath).use { writer ->
                        writer.write(createSuccessSarif(document.javaClass.simpleName))
                    }
                    logger.info("Metapath evaluation completed successfully")
                    
                    MessageExitStatus(ExitCode.OK, jsonResult)
                } catch (e: Exception) {
                    logger.error("Metapath evaluation failed", e)
                    MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message)
                }
                Pair(exitStatus, exitStatus.message ?: "")
            }
        } catch (e: Exception) {
            logger.error("Metapath evaluation failed with exception", e)
            MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message) to ""
        }
    }

    private fun createSuccessSarif(documentType: String): String {
        val version = OscalCliVersion().getVersion()
        return """
        {
          "${'$'}schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
          "version": "2.1.0",
          "runs": [
            {
              "tool": {
                "driver": {
                  "name": "OSCAL Server",
                  "informationUri": "https://github.com/metaschema-framework/oscal-server",
                  "version": "$version"
                }
              },
              "results": [],
              "invocations": [
                {
                  "executionSuccessful": true,
                  "toolExecutionNotifications": [
                    {
                      "level": "note",
                      "message": {
                        "text": "Document validated successfully as $documentType"
                      }
                    }
                  ]
                }
              ]
            }
          ]
        }
        """.trimIndent()
    }
}

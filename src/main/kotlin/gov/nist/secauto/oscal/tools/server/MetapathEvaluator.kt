/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath

import gov.nist.secauto.metaschema.cli.processor.ExitCode
import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import gov.nist.secauto.metaschema.cli.processor.MessageExitStatus
import gov.nist.secauto.metaschema.core.metapath.DynamicContext
import gov.nist.secauto.metaschema.core.metapath.MetapathExpression
import gov.nist.secauto.metaschema.core.metapath.StaticContext
import gov.nist.secauto.metaschema.core.metapath.item.IItem
import gov.nist.secauto.metaschema.core.metapath.item.JsonItemWriter
import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItem
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

    fun evaluateMetapath(
        inputPath: Path,
        expression: String,
        module: String,
        oscalDir: Path
    ): Pair<ExitStatus, String> {
        logger.info("Evaluating metapath expression on: $inputPath")
        return try {
                val exitStatus = try {
                    // Load document as node item
                    logger.debug("Loading document as node item...")
                    val loader = context.newBoundLoader()
                    val item: INodeItem = loader.loadAsNodeItem(inputPath.toUri())
                    logger.info("Document loaded successfully")

                    // Set up metapath context
                    logger.debug("Setting up metapath context...")
                    val staticContext = StaticContext.builder()
                        .defaultModelNamespace("http://csrc.nist.gov/ns/oscal/1.0")
                        .build()
                    val dynamicContext = DynamicContext(staticContext)

                    // Compile and evaluate expression
                    logger.debug("Compiling metapath expression: $expression")
                    val compiledMetapath = try {
                        MetapathExpression.compile(expression, staticContext)
                    } catch (ex: Exception) {
                        logger.error("Failed to compile metapath expression", ex)
                        return Pair(
                            MessageExitStatus(ExitCode.FAIL, "Failed to compile metapath expression: ${ex.message}"),
                            ""
                        )
                    }

                    logger.debug("Evaluating metapath expression...")
                    val sequence = compiledMetapath.evaluate<INodeItem>(item, dynamicContext)

                    // Write result to string
                    logger.debug("Writing evaluation results...")
                    val stringWriter = java.io.StringWriter()
                    java.io.PrintWriter(stringWriter).use { writer ->
                        val itemWriter = JsonItemWriter(writer, context)
                        itemWriter.writeSequence(sequence)
                    }

                    val result = stringWriter.toString()
                    logger.info("Metapath evaluation completed successfully")
                    MessageExitStatus(ExitCode.OK, result)
                } catch (e: Exception) {
                    logger.error("Metapath evaluation failed", e)
                    MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message)
                }
                Pair(exitStatus, exitStatus.message ?: "")
            
        } catch (e: Exception) {
            logger.error("Metapath evaluation failed with exception", e)
            MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message) to ""
        }
    }

}

/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager
import java.nio.file.Files
import kotlin.io.path.appendText
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.Path.of

class RequestHandler(
    private val urlProcessor: UrlProcessor,
    private val commandExecutor: CommandExecutor,
    private val responseHandler: ResponseHandler,
    private val oscalDir: Path
) : CoroutineScope {
    private val logger: Logger = LogManager.getLogger(RequestHandler::class.java)
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    fun handleHealthCheck(ctx: RoutingContext) {
        val response = io.vertx.core.json.JsonObject()
            .put("status", "healthy")
            .put("activeWorkers", commandExecutor.getActiveWorkers())
        
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(response.encode())
    }

    fun handleQueryRequest(ctx: RoutingContext) {
        try {
            logger.info("Handling Query request")
            val encodedContent = ctx.queryParam("document").firstOrNull()
            val expression = ctx.queryParam("expression").firstOrNull()
            val module = ctx.queryParam("module").firstOrNull()
            
            if (encodedContent == null || expression == null || module == null) {
                responseHandler.sendErrorResponse(ctx, 400, "Missing required parameters")
                return
            }
            
            val content = urlProcessor.processUrl(encodedContent)
            val result = commandExecutor.evaluateMetapath(Paths.get(content), expression, module)
            logger.info(result.second)
            ctx.response().putHeader("Content-Type", "application/json")
            responseHandler.sendSuccessResponse(ctx, result.first, result.second)
        } catch (e: Exception) {
            logger.error("Error handling request", e)
            responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
        }
    }

    suspend fun handleValidateRequest(ctx: RoutingContext) {
        try {
            logger.info("Handling Validate request")
            val encodedContent = ctx.queryParam("document").firstOrNull()
            val flags = ctx.queryParam("flags").toSet()
            val constraints = ctx.queryParam("constraint").map { urlProcessor.processUrl(it) }
            val module = ctx.queryParam("module").firstOrNull()
            
            if (encodedContent == null) {
                responseHandler.sendErrorResponse(ctx, 400, "content parameter is missing")
                return
            }
            
            val content = urlProcessor.processUrl(encodedContent)
            val inputPath = Paths.get(content)
            
            try {
                val result = commandExecutor.validateDocument(
                    inputPath = inputPath,
                    flags = flags,
                    constraints = constraints.map { Paths.get(it) },
                    module = module
                )
                
                // Read SARIF output if it exists
                val sarifContent = if (Files.exists(of(result.second))) {
                    Files.readString(of(result.second))
                } else {
                    // If SARIF file wasn't created, return command output
                    result.second
                }
                
                ctx.response().putHeader("Content-Type", "application/sarif+json")
                responseHandler.sendSuccessResponse(ctx, result.first, sarifContent)
                Files.deleteIfExists(of(result.second))
            } finally {
                // Clean up temporary SARIF file
            }
        } catch (e: Exception) {
            logger.error("Error handling request", e)
            responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
        }
    }

    fun handleResolveRequest(ctx: RoutingContext) {
        try {
            val encodedContent = ctx.queryParam("document").firstOrNull()
            if (encodedContent != null) {
                val content = urlProcessor.processUrl(encodedContent)
                val acceptHeader = ctx.request().getHeader("Accept")
                val formatParam = ctx.queryParam("format").firstOrNull()
                val format = responseHandler.mapMimeTypeToFormat(acceptHeader, formatParam)
                
                // Generate output file path
                val outputFile = of(oscalDir.toString(), "resolve-${System.nanoTime()}.tmp")
                
                val result = commandExecutor.resolveProfile(Paths.get(content), outputFile, format)
                
                // Read output and clean up
                val output = if (Files.exists(outputFile)) Files.readString(outputFile) else result.second
                Files.deleteIfExists(outputFile)
                
                logger.info(result.second)
                ctx.response().putHeader("Content-Type", responseHandler.mapFormatToMimeType(format))
                responseHandler.sendSuccessResponse(ctx, result.first, output)
            } else {
                responseHandler.sendErrorResponse(ctx, 400, "content parameter is missing")
            }
        } catch (e: Exception) {
            logger.error("Error handling request", e)
            responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
        }
    }

    fun handleConvertRequest(ctx: RoutingContext) {
        try {
            val encodedContent = ctx.queryParam("document").firstOrNull()
            if (encodedContent != null) {
                val content = urlProcessor.processUrl(encodedContent)
                val acceptHeader = ctx.request().getHeader("Accept")
                val formatParam = ctx.queryParam("format").firstOrNull()
                val format = responseHandler.mapMimeTypeToFormat(acceptHeader, formatParam)
                
                // Generate output file path
                val outputFile = of(oscalDir.toString(), "convert-${System.nanoTime()}.tmp")
                
                val result = commandExecutor.convertDocument(Paths.get(content), outputFile, format)
                
                // Read output and clean up
                val output = if (Files.exists(outputFile)) Files.readString(outputFile) else result.second
                Files.deleteIfExists(outputFile)
                
                logger.info(result.second)
                ctx.response().putHeader("Content-Type", responseHandler.mapFormatToMimeType(format))
                responseHandler.sendSuccessResponse(ctx, result.first, output)
            } else {
                responseHandler.sendErrorResponse(ctx, 400, "content parameter is missing")
            }
        } catch (e: Exception) {
            logger.error("Error handling request", e)
            responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
        }
    }

    private fun getExtensionFromContentType(contentType: String?): String {
        return when (contentType?.lowercase()) {
            "application/json" -> ".json"
            "text/yaml" -> ".yaml"
            "text/xml" -> ".xml"
            else -> ".tmp"
        }
    }

    private fun storeUploadedFile(body: String, prefix: String, contentType: String): Path {
        val tempFile = of(oscalDir.toString(), "$prefix-${System.nanoTime()}${getExtensionFromContentType(contentType)}")
        Files.writeString(tempFile, body)
        return tempFile
    }

    fun handleValidateFileUpload(ctx: RoutingContext) {
        launch {
            try {
                logger.info("Handling validate content request")
                val body = ctx.body().asString()
                if (body.isNullOrEmpty()) {
                    logger.error("No content provided")
                    responseHandler.sendErrorResponse(ctx, 400, "No content provided")
                    return@launch
                }

                val tempFile = storeUploadedFile(body, "validate", ctx.request().getHeader("Content-Type"))
                
                // Update the context's query parameters with the file path
                val params = ctx.queryParams()
                params.set("document", "file://${tempFile.toAbsolutePath()}")
                
                try {
                    handleValidateRequest(ctx)
                } finally {
                    Files.deleteIfExists(tempFile)
                }
            } catch (e: Exception) {
                logger.error("Error handling content request", e)
                responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
            }
        }
    }

    fun handleQueryFileUpload(ctx: RoutingContext) {
        launch {
            try {
                logger.info("Handling query content request")
                val body = ctx.body().asString()
                if (body.isNullOrEmpty()) {
                    logger.error("No content provided")
                    responseHandler.sendErrorResponse(ctx, 400, "No content provided")
                    return@launch
                }

                val tempFile = storeUploadedFile(body, "query", ctx.request().getHeader("Content-Type"))
                
                // Update the context's query parameters with the file path
                val params = ctx.queryParams()
                params.set("document", "file://${tempFile.toAbsolutePath()}")
                
                try {
                    handleQueryRequest(ctx)
                } finally {
                    Files.deleteIfExists(tempFile)
                }
            } catch (e: Exception) {
                logger.error("Error handling content request", e)
                responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
            }
        }
    }

    fun handleResolveFileUpload(ctx: RoutingContext) {
        launch {
            try {
                logger.info("Handling resolve content request")
                val body = ctx.body().asString()
                if (body.isNullOrEmpty()) {
                    logger.error("No content provided")
                    responseHandler.sendErrorResponse(ctx, 400, "No content provided")
                    return@launch
                }

                val tempFile = storeUploadedFile(body, "resolve", ctx.request().getHeader("Content-Type"))
                
                // Update the context's query parameters with the file path
                val params = ctx.queryParams()
                params.set("document", "file://${tempFile.toAbsolutePath()}")
                
                try {
                    handleResolveRequest(ctx)
                } finally {
                    Files.deleteIfExists(tempFile)
                }
            } catch (e: Exception) {
                logger.error("Error handling content request", e)
                responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
            }
        }
    }

    fun handleConvertFileUpload(ctx: RoutingContext) {
        launch {
            try {
                logger.info("Handling convert content request")
                val body = ctx.body().asString()
                if (body.isNullOrEmpty()) {
                    logger.error("No content provided")
                    responseHandler.sendErrorResponse(ctx, 400, "No content provided")
                    return@launch
                }

                val tempFile = storeUploadedFile(body, "convert", ctx.request().getHeader("Content-Type"))
                
                // Update the context's query parameters with the file path
                val params = ctx.queryParams()
                params.set("document", "file://${tempFile.toAbsolutePath()}")
                
                try {
                    handleConvertRequest(ctx)
                } finally {
                    Files.deleteIfExists(tempFile)
                }
            } catch (e: Exception) {
                logger.error("Error handling content request", e)
                responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
            }
        }
    }
}

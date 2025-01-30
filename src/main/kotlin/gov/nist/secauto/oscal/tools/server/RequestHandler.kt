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

    suspend fun handleQueryRequest(ctx: RoutingContext) {
        try {
            logger.info("Handling Query request")
            val encodedContent = ctx.queryParam("document").firstOrNull()
            val expression = ctx.queryParam("expression").firstOrNull()
            val module = ctx.queryParam("module").firstOrNull()
            if (encodedContent != null && expression != null && module != null) {
                val content = urlProcessor.processUrl(encodedContent)
                val args = mutableListOf("metaschema", "metapath", "eval")
                args.add("-i")
                args.add(content)
                args.add("-e")
                args.add(expression)
                args.add("-m")
                args.add(module)
                val result = commandExecutor.executeCommand(args)
                logger.info(result.second)
                responseHandler.sendSuccessResponse(ctx, result.first, result.second)
            } else {
                responseHandler.sendErrorResponse(ctx, 400, "content parameter is missing")
            }
        } catch (e: Exception) {
            logger.error("Error handling request", e)
            responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
        }
    }

    suspend fun handleValidateRequest(ctx: RoutingContext) {
        try {
            logger.info("Handling Validate request")
            val encodedContent = ctx.queryParam("document").firstOrNull()
            val encodedModule = ctx.queryParam("module").firstOrNull()
            val constraint = ctx.queryParam("constraint")
            val flags = ctx.queryParam("flags")
            if (encodedContent != null) {
                val content = urlProcessor.processUrl(encodedContent)
                val args = mutableListOf("validate")
                encodedModule?.let { module ->
                    if (module == "http://csrc.nist.gov/ns/oscal/metaschema/1.0") {
                        args[0] = "metaschema"
                        args.add("validate")
                    } else {
                        args[0] = "metaschema"
                        args.add("validate-content")
                    }
                }
                args.add(content)
                args.add("--show-stack-trace")
                constraint.forEach { constraint_document ->
                    args.add("-c")
                    args.add(urlProcessor.processUrl(constraint_document))
                }
                flags.forEach { flag ->
                    args.add(responseHandler.flagToParam(flag))
                }
                val result = commandExecutor.executeCommand(args)
                logger.info(result.second)
                responseHandler.sendSuccessResponse(ctx, result.first, result.second)
            } else {
                responseHandler.sendErrorResponse(ctx, 400, "content parameter is missing")
            }
        } catch (e: Exception) {
            logger.error("Error handling request", e)
            responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
        }
    }

    suspend fun handleResolveRequest(ctx: RoutingContext) {
        try {
            val encodedContent = ctx.queryParam("document").firstOrNull()
            if (encodedContent != null) {
                val content = urlProcessor.processUrl(encodedContent)
                val acceptHeader = ctx.request().getHeader("Accept")
                val formatParam = ctx.queryParam("format").firstOrNull()
                val format = responseHandler.mapMimeTypeToFormat(acceptHeader, formatParam)
                
                // Generate output file path
                val outputFile = of(oscalDir.toString(), "resolve-${System.nanoTime()}.tmp")
                
                // Build command: resolve-profile --to=FORMAT source-file destination-file
                val args = mutableListOf("resolve-profile", "--to=$format", content, outputFile.toString())
                val result = commandExecutor.executeCommand(args)
                
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
            logger.error("Error handling CLI request", e)
            responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
        }
    }

    suspend fun handleConvertRequest(ctx: RoutingContext) {
        try {
            val encodedContent = ctx.queryParam("document").firstOrNull()
            if (encodedContent != null) {
                val content = urlProcessor.processUrl(encodedContent)
                val acceptHeader = ctx.request().getHeader("Accept")
                val formatParam = ctx.queryParam("format").firstOrNull()
                val format = responseHandler.mapMimeTypeToFormat(acceptHeader, formatParam)
                
                // Generate output file path
                val outputFile = of(oscalDir.toString(), "convert-${System.nanoTime()}.tmp")
                
                // Build command: convert --to=FORMAT source-file destination-file
                val args = mutableListOf("convert", "--to=$format", content, outputFile.toString())
                val result = commandExecutor.executeCommand(args)
                
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
            logger.error("Error handling CLI request", e)
            responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
        }
    }

    private suspend fun storeUploadedFile(body: String, prefix: String, contentType: String): Path {
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

    private fun getExtensionFromContentType(contentType: String?): String {
        return when (contentType?.lowercase()) {
            "application/json" -> ".json"
            "text/yaml" -> ".yaml"
            "text/xml" -> ".xml"
            else -> ".tmp"
        }
    }
}

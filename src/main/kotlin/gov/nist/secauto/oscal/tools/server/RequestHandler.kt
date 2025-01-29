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
                val result = commandExecutor.executeCommand(parseCommandToArgs("resolve-profile $content --to=$format"))
                logger.info(result.second)
                ctx.response().putHeader("Content-Type", responseHandler.mapFormatToMimeType(format))
                responseHandler.sendSuccessResponse(ctx, result.first, result.second)
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
                val result = commandExecutor.executeCommand(parseCommandToArgs("convert $content --to=$format"))
                logger.info(result.second)
                ctx.response().putHeader("Content-Type", responseHandler.mapFormatToMimeType(format))
                responseHandler.sendSuccessResponse(ctx, result.first, result.second)
            } else {
                responseHandler.sendErrorResponse(ctx, 400, "content parameter is missing")
            }
        } catch (e: Exception) {
            logger.error("Error handling CLI request", e)
            responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
        }
    }

    fun handleValidateFileUpload(ctx: RoutingContext) {
        launch {
            try {
                logger.info("Handling file upload request")
                val fileUploads = ctx.fileUploads()
                if (fileUploads.isEmpty()) {
                    responseHandler.sendErrorResponse(ctx, 400, "No file uploaded")
                    return@launch
                }

                val upload = fileUploads.first()
                val flags = ctx.queryParam("flags")
                val encodedModule = ctx.queryParam("module").firstOrNull()
                
                // Prepare CLI arguments
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
                args.add(upload.uploadedFileName())
                args.add("--show-stack-trace")
                flags.forEach { flag ->
                    args.add(responseHandler.flagToParam(flag))
                }

                val result = commandExecutor.executeCommand(args)
                logger.info("Validation result: ${result.second}")

                if (result.first.exitCode == gov.nist.secauto.metaschema.cli.processor.ExitCode.OK) {
                    responseHandler.sendSuccessResponse(ctx, result.first, result.second)
                } else {
                    responseHandler.sendErrorResponse(ctx, 400, result.first.exitCode.toString())
                }
            } catch (e: Exception) {
                logger.error("Error handling file upload request", e)
                responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
            }
        }
    }

    fun handleQueryFileUpload(ctx: RoutingContext) {
        launch {
            try {
                logger.info("Handling Query file upload request")
                val fileUploads = ctx.fileUploads()
                if (fileUploads.isEmpty()) {
                    responseHandler.sendErrorResponse(ctx, 400, "No file uploaded")
                    return@launch
                }

                val upload = fileUploads.first()
                val expression = ctx.queryParam("expression").firstOrNull()
                val module = ctx.queryParam("module").firstOrNull()

                if (expression != null && module != null) {
                    val args = mutableListOf("metaschema", "metapath", "eval")
                    args.add("-i")
                    args.add(upload.uploadedFileName())
                    args.add("-e")
                    args.add(expression)
                    args.add("-m")
                    args.add(module)

                    val result = commandExecutor.executeCommand(args)
                    logger.info(result.second)
                    responseHandler.sendSuccessResponse(ctx, result.first, result.second)
                } else {
                    responseHandler.sendErrorResponse(ctx, 400, "Missing expression or module parameter")
                }
            } catch (e: Exception) {
                logger.error("Error handling file upload request", e)
                responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
            }
        }
    }

    fun handleResolveFileUpload(ctx: RoutingContext) {
        launch {
            try {
                logger.info("Handling Resolve file upload request")
                val fileUploads = ctx.fileUploads()
                if (fileUploads.isEmpty()) {
                    responseHandler.sendErrorResponse(ctx, 400, "No file uploaded")
                    return@launch
                }

                val upload = fileUploads.first()
                val acceptHeader = ctx.request().getHeader("Accept")
                val formatParam = ctx.queryParam("format").firstOrNull()
                val format = responseHandler.mapMimeTypeToFormat(acceptHeader, formatParam)

                val result = commandExecutor.executeCommand(
                    parseCommandToArgs("resolve-profile ${upload.uploadedFileName()} --to=$format")
                )
                
                logger.info(result.second)
                ctx.response().putHeader("Content-Type", responseHandler.mapFormatToMimeType(format))
                responseHandler.sendSuccessResponse(ctx, result.first, result.second)
            } catch (e: Exception) {
                logger.error("Error handling file upload request", e)
                responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
            }
        }
    }

    fun handleConvertFileUpload(ctx: RoutingContext) {
        launch {
            try {
                logger.info("Handling Convert file upload request")
                val fileUploads = ctx.fileUploads()
                if (fileUploads.isEmpty()) {
                    responseHandler.sendErrorResponse(ctx, 400, "No file uploaded")
                    return@launch
                }

                val upload = fileUploads.first()
                val acceptHeader = ctx.request().getHeader("Accept")
                val formatParam = ctx.queryParam("format").firstOrNull()
                val format = responseHandler.mapMimeTypeToFormat(acceptHeader, formatParam)

                val result = commandExecutor.executeCommand(
                    parseCommandToArgs("convert ${upload.uploadedFileName()} --to=$format")
                )
                
                logger.info(result.second)
                ctx.response().putHeader("Content-Type", responseHandler.mapFormatToMimeType(format))
                responseHandler.sendSuccessResponse(ctx, result.first, result.second)
            } catch (e: Exception) {
                logger.error("Error handling file upload request", e)
                responseHandler.sendErrorResponse(ctx, 500, "Internal server error")
            }
        }
    }

    private fun parseCommandToArgs(command: String): List<String> {
        return command.split("\\s+".toRegex()).filter { it.isNotBlank() }
    }
}

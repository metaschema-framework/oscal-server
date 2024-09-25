package gov.nist.secauto.oscal.tools.server

import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import gov.nist.secauto.oscal.tools.cli.core.CLI
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.openapi.OpenAPILoaderOptions
import io.vertx.ext.web.openapi.RouterBuilder
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import io.vertx.ext.web.handler.StaticHandler
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.ThreadContext
import java.util.UUID
import gov.nist.secauto.oscal.tools.server.logging.RequestLogHolder

class OscalVerticle : CoroutineVerticle() {
    private val scope = CoroutineScope(SupervisorJob())
    private val logger: Logger = LogManager.getLogger(OscalVerticle::class.java)

    override suspend fun start() {
        val router = createRouter()
        router.get("/health").handler { ctx ->
            ctx.response().setStatusCode(200).end("OK")
        }
        router.route("/").handler(StaticHandler.create())
        startHttpServer(router)
    }

    private suspend fun createRouter(): Router {
        val options = OpenAPILoaderOptions()
        val routerBuilder = RouterBuilder.create(vertx, "openapi.yaml", options).await()
        routerBuilder.operation("oscal").handler(::handleCliRequest)
        return routerBuilder.createRouter()
    }

    private fun startHttpServer(router: Router) {
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888)
            .onSuccess { server -> 
                logger.info("HTTP server started on port ${server.actualPort()}")
            }
            .onFailure { error ->
                logger.error("Failed to start HTTP server: ${error.message}", error)
            }
    }


    private fun parseCommandToArgs(command: String): Array<String> {
        return command.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .toTypedArray()
    }

    private fun captureCliOutput(block: () -> ExitStatus): Pair<ExitStatus, String> {
        try {

            val result = block()            
            return Pair(result, "output")
        } finally {

        }
    }

    private fun sendSuccessResponse(ctx: RoutingContext, exitCode: Int, status: String, output: String) {
        val responseJson = JsonObject()
            .put("exitCode", exitCode)
            .put("status", status)
            .put("output", output)

        ctx.response()
            .putHeader("content-type", "application/json")
            .end(responseJson.encode())
    }

    private fun sendErrorResponse(ctx: RoutingContext, statusCode: Int, message: String) {
        ctx.response()
            .setStatusCode(statusCode)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("error", message).encode())
    }

    private fun handleCliRequest(ctx: RoutingContext) {
        val command = ctx.queryParam("command").firstOrNull()
        if (command.isNullOrEmpty()) {
            sendErrorResponse(ctx, 400, "Missing 'command' query parameter")
            return
        }

        scope.launch(vertx.dispatcher()) {
            try {
                val requestId = UUID.randomUUID().toString()
                ThreadContext.put("requestId", requestId)
                
                val args = parseCommandToArgs(command)
                val (exitStatus, output) = withContext(Dispatchers.IO) {
                    captureCliOutput(requestId) { CLI.runCli(*args) }
                }
                
                val capturedLogs = RequestLogHolder.logs.remove(requestId)?.toString() ?: ""
                
                if (output.isNotEmpty() || capturedLogs.isNotEmpty()) {
                    sendSuccessResponse(ctx, exitStatus.exitCode.statusCode, exitStatus.exitCode.name, output, capturedLogs)
                } else {
                    logger.warn("CLI operation produced no output for command: $command")
                    sendErrorResponse(ctx, 500, capturedLogs)
                }
            } catch (e: Exception) {
                logger.error("Error processing CLI request: ${e.message}", e)
                sendErrorResponse(ctx, 500, "Internal Server Error: ${e.message}")
            } finally {
                ThreadContext.clearAll()
            }
        }
    }

    private fun captureCliOutput(requestId: String, block: () -> ExitStatus): Pair<ExitStatus, String> {
        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream)
        
        val originalOut = System.out
        val originalErr = System.err
        
        System.setOut(printStream)
        System.setErr(printStream)
        
        try {
            ThreadContext.put("requestId", requestId)
            val result = block()
            return Pair(result, outputStream.toString())
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
            ThreadContext.clearAll()
        }
    }

    private fun sendSuccessResponse(ctx: RoutingContext, exitCode: Int, status: String, output: String, logs: String) {
        val responseJson = JsonObject()
            .put("exitCode", exitCode)
            .put("status", status)
            .put("output", logs)

        ctx.response()
            .putHeader("content-type", "application/json")
            .end(responseJson.encode())
    }
    
}
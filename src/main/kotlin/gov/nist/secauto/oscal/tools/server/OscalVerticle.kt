package gov.nist.secauto.oscal.tools.server

import gov.nist.secauto.oscal.tools.cli.core.CLI
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.openapi.OpenAPILoaderOptions
import io.vertx.ext.web.openapi.RouterBuilder
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class OscalVerticle : CoroutineVerticle() {

    companion object {
        private val logger: Logger = LogManager.getLogger(OscalVerticle::class.java)
    }

    override suspend fun start() {
        val router = createRouter()
        startHttpServer(router)
    }

    private suspend fun createRouter(): Router {
        val options = OpenAPILoaderOptions()
        val routerBuilder = RouterBuilder.create(vertx, "openapi.yaml", options).coAwait()
        routerBuilder.operation("runCli").handler(::handleCliRequest)
        return routerBuilder.createRouter()
    }

    private fun startHttpServer(router: Router) {
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888)
            .onSuccess { 
                logger.info("HTTP server started on port 8888")
            }
            .onFailure { error ->
                logger.error("Failed to start HTTP server", error)
            }
    }

    private fun handleCliRequest(ctx: RoutingContext) {
        val command = ctx.queryParam("command").firstOrNull()
        if (command.isNullOrEmpty()) {
            sendErrorResponse(ctx, 400, "Missing 'command' query parameter")
            return
        }

        logger.info("Received CLI request with command: $command")

        launch(vertx.dispatcher()) {
            try {
                val args = parseCommandToArgs(command)
                val result = CLI.runCli(*args)
                sendSuccessResponse(ctx, result.exitCode.statusCode, result.exitCode.name)
            } catch (e: Exception) {
                logger.error("Error executing CLI command", e)
                sendErrorResponse(ctx, 500, "Internal Server Error")
            }
        }
    }

    private fun parseCommandToArgs(command: String): Array<String> {
        return command.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .toTypedArray()
    }

    private fun sendSuccessResponse(ctx: RoutingContext, exitCode: Int, status: String) {
        val responseJson = JsonObject()
            .put("exitCode", exitCode)
            .put("status", status)

        logger.info("CLI execution completed with exit code: $exitCode")

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
}
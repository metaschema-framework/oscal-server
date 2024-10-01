package gov.nist.secauto.oscal.tools.server
import io.vertx.ext.web.handler.StaticHandler
import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.openapi.OpenAPILoaderOptions
import io.vertx.ext.web.openapi.RouterBuilder
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import gov.nist.secauto.oscal.tools.server.commands.OscalCommandExecutor
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream

class OscalVerticle : CoroutineVerticle() {
    private val logger: Logger = LogManager.getLogger(OscalVerticle::class.java)

    override suspend fun start() {
        val router = createRouter()
        startHttpServer(router)
    }

    private suspend fun createRouter(): Router {
        logger.info("Creating router")
        val options = OpenAPILoaderOptions()
        val routerBuilder = RouterBuilder.create(vertx, "openapi.yaml", options).coAwait()
        logger.info("Router builder created")
        routerBuilder.operation("oscal").handler { ctx -> handleCliRequest(ctx) }
        val router = routerBuilder.createRouter()
        router.route("/*").handler(StaticHandler.create("webroot"))        
        return router;
    }

    private suspend fun startHttpServer(router: Router) {
        try {
            val server = vertx.createHttpServer()
                .requestHandler(router)
                .listen(8888)
                .coAwait()
            logger.info("HTTP server started on port ${server.actualPort()}")
        } catch (e: Exception) {
            logger.error("Failed to start HTTP server", e)
        }
    }

    private fun handleCliRequest(ctx: RoutingContext) {
        // Launch a coroutine to handle the request
        launch {
            try {
                logger.info("Handling CLI request")
                val command = ctx.queryParam("command").firstOrNull()
                if (command != null) {
                    val args = parseCommandToArgs(command)
                    val (exitStatus, streams) = executeCommand(args)
                    val (output, errors, files) = streams
                    sendSuccessResponse(ctx, exitStatus, output, errors, files)
                } else {
                    sendErrorResponse(ctx, 400, "Command parameter is missing")
                }
            } catch (e: Exception) {
                logger.error("Error handling CLI request", e)
                sendErrorResponse(ctx, 500, "Internal server error")
            }
        }
    }

    private fun parseCommandToArgs(command: String): List<String> {
        return command.split("\\s+".toRegex()).filter { it.isNotBlank() }
    }

    private suspend fun executeCommand(args: List<String>): Pair<ExitStatus, Triple<String, String, String>> {
        return withContext(vertx.dispatcher()) {
            val outputStream = ByteArrayOutputStream()
            val errorStream = ByteArrayOutputStream()
            val fileStream = ByteArrayOutputStream()
            val oscalCommandExecutor = OscalCommandExecutor(args, outputStream, errorStream, fileStream)
            val exitStatus = oscalCommandExecutor.execute()
            Pair(exitStatus, Triple(outputStream.toString(), errorStream.toString(), fileStream.toString()))
        }
    }

    private fun sendSuccessResponse(ctx: RoutingContext, exitStatus: ExitStatus, errors: String, output: String, files: String) {
        val responseJson = JsonObject()
            .put("errors", errors)
            .put("output", output)
            .put("files", files)

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
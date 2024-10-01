package gov.nist.secauto.oscal.tools.server
import java.util.UUID
import io.vertx.ext.web.handler.StaticHandler
import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.openapi.OpenAPILoaderOptions
import io.vertx.ext.web.openapi.RouterBuilder
import io.vertx.core.json.JsonObject
import io.vertx.core.file.FileSystem
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import gov.nist.secauto.oscal.tools.server.commands.OscalCommandExecutor
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

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
                    val (exitStatus, sarif) = executeCommand(args)
                    sendSuccessResponse(ctx, exitStatus, sarif)
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
    private suspend fun executeCommand(args: List<String>): Pair<ExitStatus, String> {
        return withContext(vertx.dispatcher()) {
            val outputStream = ByteArrayOutputStream()
            val errorStream = ByteArrayOutputStream()
            val fileStream = ByteArrayOutputStream()
            val command = args[0]
            
            // Get the webroot path
            val resource: URL = javaClass.getResource("/webroot") 
                ?: throw IllegalStateException("Webroot directory not found")
            val uri: URI = resource.toURI()
            
            val webrootPath: Path = when {
                uri.scheme == "jar" -> {
                    val fs = FileSystems.newFileSystem(uri, emptyMap<String, Any>())
                    fs.getPath("/webroot")
                }
                else -> Paths.get(uri)
            }
    
            // Ensure webroot directory exists
            if (!Files.exists(webrootPath) || !Files.isDirectory(webrootPath)) {
                throw IllegalStateException("Invalid webroot path: $webrootPath")
            }
    
            // Create a mutable list from args
            val mutableArgs = args.toMutableList()
    
            // Generate SARIF file name and path
            val guid = UUID.randomUUID().toString()
            val sarifFileName = "${guid}.sarif"
            val sarifFilePath = webrootPath.resolve(sarifFileName).toString()
    
            // Add output file argument
            mutableArgs.add("--sarif-include-pass")
            mutableArgs.add("-o")
            mutableArgs.add(sarifFilePath)
    
            val oscalCommandExecutor = OscalCommandExecutor(command, mutableArgs, outputStream, errorStream, fileStream)
            val exitStatus = oscalCommandExecutor.execute()
    
            val sarifContent = try {
                File(sarifFilePath).readText()
            } catch (e: Exception) {
                "{result:'sarif failed', error: '${e.message}'}"
            }
    
            Pair(exitStatus, sarifContent)
        }
    }

    private fun sendSuccessResponse(ctx: RoutingContext, exitStatus: ExitStatus, output: String) {

        ctx.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(output)
    }

    private fun sendErrorResponse(ctx: RoutingContext, statusCode: Int, message: String) {
        ctx.response()
            .setStatusCode(statusCode)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("error", message).encode())
    }
}
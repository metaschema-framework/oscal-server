package gov.nist.secauto.oscal.tools.server
import io.vertx.core.Vertx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.UUID
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.BodyHandler
import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import gov.nist.secauto.metaschema.cli.processor.MessageExitStatus
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.openapi.OpenAPILoaderOptions
import io.vertx.ext.web.openapi.RouterBuilder
import io.vertx.core.json.JsonObject
import io.vertx.core.file.FileSystem
import io.vertx.core.VertxOptions
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
import java.net.URLDecoder
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import io.vertx.kotlin.coroutines.awaitBlocking
import kotlin.io.path.appendText

class OscalVerticle : CoroutineVerticle() {
    private val logger: Logger = LogManager.getLogger(OscalVerticle::class.java)

    override suspend fun start() {
        VertxOptions().setEventLoopPoolSize(8)
        val router = createRouter()
        startHttpServer(router)
    }


    private suspend fun createRouter(): Router {
        logger.info("Creating router")
        val options = OpenAPILoaderOptions()
        val routerBuilder = RouterBuilder.create(vertx, "webroot/openapi.yaml", options).coAwait()
        logger.info("Router builder created")
        

        // Add BodyHandler to handle file uploads
        routerBuilder.rootHandler(BodyHandler.create("webroot"))
        
        routerBuilder.operation("validateUpload").handler { ctx -> handleValidateFileUpload(ctx) }
        routerBuilder.operation("validate").handler { ctx -> handleValidateRequest(ctx) }
        routerBuilder.operation("resolve").handler { ctx -> handleResolveRequest(ctx) }
        routerBuilder.operation("convert").handler { ctx -> handleConvertRequest(ctx) }
        val router = routerBuilder.createRouter()
        router.route("/*").handler(StaticHandler.create("webroot"))
        return router
    }
    private fun handleValidateFileUpload(ctx: RoutingContext) {
        logger.info("Handling file upload request!")
        launch {
            try {
                logger.info("Handling file upload request in the background")
                val body = ctx.body().asString()
                logger.info("Received body: $body")
    
                if (body.isNotEmpty()) {
                    // Create a temporary file
                    val tempFile = kotlin.io.path.createTempFile(prefix = "upload", suffix = ".tmp")
                    val tempFilePath = tempFile.toAbsolutePath()
                    logger.info("Created temporary file: $tempFilePath")
    
                    // Write the body content to the temporary file
                    tempFile.appendText(body)
                    logger.info("Wrote body content to temporary file")
    
                    // Use async for parallelism
                    val result = async {
                        executeCommand(listOf("validate", tempFilePath.toString()))
                    }.await() // Wait for the result of the async execution
                    
                    logger.info("Validation result: ${result.second}")
                    sendSuccessResponse(ctx, result.first, result.second)
                    
                    // Clean up the temporary file
                } else {
                    sendErrorResponse(ctx, 400, "No content in request body")
                }
            } catch (e: Exception) {
                logger.error("Error handling file upload request", e)
                sendErrorResponse(ctx, 500, "Internal server error")
            }
        }
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

    private fun handleValidateRequest(ctx: RoutingContext) {
        launch {
            try {
                logger.info("Handling Validate request")
                val encodedContent = ctx.queryParam("content").firstOrNull()
                val content = encodedContent?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                                if (content != null) {
                    // Use async for parallelism
                    val result = async {
                        executeCommand(parseCommandToArgs("validate "+content))
                    }.await() // Wait for the result of the async execution
                    logger.info(result.second)
                    sendSuccessResponse(ctx, result.first, result.second)
                } else {
                    sendErrorResponse(ctx, 400, "content parameter is missing")
                }
            } catch (e: Exception) {
                logger.error("Error handling CLI request", e)
                sendErrorResponse(ctx, 500, "Internal server error")
            }
        }
    }
    private fun handleResolveRequest(ctx: RoutingContext) {
        launch {
            try {
                logger.info("Handling Resolve request")
                val encodedContent = ctx.queryParam("content").firstOrNull()
                val content = encodedContent?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                val acceptHeader = ctx.request().getHeader("Accept")
                val format = mapMimeTypeToFormat(acceptHeader)
                
                if (content != null) {
                    // Use async for parallelism
                    val result = async {
                        executeCommand(parseCommandToArgs("resolve-profile $content --to=$format"))
                    }.await() // Wait for the result of the async execution
                    logger.info(result.second)
                    sendSuccessResponse(ctx, result.first, result.second)
                } else {
                    sendErrorResponse(ctx, 400, "content parameter is missing")
                }
            } catch (e: Exception) {
                logger.error("Error handling CLI request", e)
                sendErrorResponse(ctx, 500, "Internal server error")
            }
        }
    }
    private fun mapMimeTypeToFormat(mimeType: String?): String {
        return when (mimeType) {
            "application/json" -> "JSON"
            "application/xml" -> "XML"
            "application/x-yaml" -> "YAML"
            else -> "JSON" // Default to JSON if no valid MIME type is provided
        }
    }
    private fun handleConvertRequest(ctx: RoutingContext) {
        launch {
            try {
                logger.info("Handling Convert request")
                val encodedContent = ctx.queryParam("content").firstOrNull()
                val content = encodedContent?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                val acceptHeader = ctx.request().getHeader("Accept")
                val format = mapMimeTypeToFormat(acceptHeader)
                
                if (content != null) {
                    // Use async for parallelism
                    val result = async {
                        executeCommand(parseCommandToArgs("convert $content --to=$format"))
                    }.await() // Wait for the result of the async execution
                    logger.info(result.second)
                    sendSuccessResponse(ctx, result.first, result.second)
                } else {
                    sendErrorResponse(ctx, 400, "content parameter is missing")
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
            awaitBlocking {
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
                logger.info("SARIF file path: $sarifFilePath")
                if(mutableArgs.contains(("-o"))){
                    throw Error("Do not specify sarif file")
                }
                if (mutableArgs[0]=="validate"){
                    if(!mutableArgs.contains(("--sarif-include-pass"))){
                        mutableArgs.add("--sarif-include-pass")
                    }
                    mutableArgs.add("-o")    
                }
                mutableArgs.add(sarifFilePath)

                val oscalCommandExecutor = OscalCommandExecutor(command, mutableArgs)
                val exitStatus = oscalCommandExecutor.execute()
        
                // Check if SARIF file was created
                if (!File(sarifFilePath).exists()) {
                    val basicSarif = createBasicSarif("code:"+exitStatus.exitCode.toString())
                    File(sarifFilePath).writeText(basicSarif)
                }
        
                Pair(exitStatus, sarifFilePath)
            }
        }
    }

    private fun createBasicSarif(errorMessage: String): String {
        return """
        {
          "${'$'}schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
          "version": "2.1.0",
          "runs": [
            {
              "tool": {
                "driver": {
                  "name": "OSCAL Tool",
                  "informationUri": "https://pages.nist.gov/OSCAL/",
                  "version": "1.0.0"
                }
              },
              "results": [
                {
                  "message": {
                    "text": "Error occurred during OSCAL command execution: $errorMessage"
                  },
                  "level": "error"
                }
              ]
            }
          ]
        }
        """.trimIndent()
    }
    private fun sendSuccessResponse(ctx: RoutingContext, exitStatus: ExitStatus, sarifFilePath: String) {
        val fileContent = File(sarifFilePath).readText()
        ctx.response()
            .setStatusCode(200) // HTTP 200 OK
            .putHeader("Content-Type", "application/json")
            .end(fileContent)
    }

    private fun sendErrorResponse(ctx: RoutingContext, statusCode: Int, message: String) {
        ctx.response()
            .setStatusCode(statusCode)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("error", message).encode())
    }
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val vertx = Vertx.vertx()
            vertx.deployVerticle(OscalVerticle())
        }
    }
}
/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

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
import java.util.concurrent.atomic.AtomicInteger

class OscalVerticle : CoroutineVerticle() {
    private val logger: Logger = LogManager.getLogger(OscalVerticle::class.java)
    private lateinit var oscalDir: Path
    private val activeWorkers = AtomicInteger(0)

    override suspend fun start() {
        VertxOptions().setEventLoopPoolSize(8)
        initializeOscalDirectory()
        val router = createRouter()
        startHttpServer(router)
    }
    private fun initializeOscalDirectory() {
        val homeDir = System.getProperty("user.home")
        oscalDir = Paths.get(homeDir, ".oscal")
        if (!Files.exists(oscalDir)) {
            Files.createDirectory(oscalDir)
        }
        logger.info("OSCAL directory initialized at: $oscalDir")
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
        routerBuilder.operation("healthCheck").handler { ctx -> handleHealthCheck(ctx) }

        val router = routerBuilder.createRouter()
        router.route("/*").handler(StaticHandler.create("webroot"))
        return router
    }

    private fun handleHealthCheck(ctx: RoutingContext) {
        val response = JsonObject()
            .put("status", "healthy")
            .put("activeWorkers", activeWorkers.get())
        
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(response.encode())
    }
    private fun processUrl(url: String): String {
        return if (url.startsWith("file://")) {
            try {
                val uri = URI(url)
                val path = when {
                    uri.authority != null -> {
                        // Remove the authority component
                        Paths.get(uri.authority + uri.path)
                    }
                    uri.path.startsWith("/") -> {
                        // Absolute path
                        Paths.get(uri.path)
                    }
                    else -> {
                        // Relative path
                        Paths.get(uri.path).toAbsolutePath()
                    }
                }
                path.toString()
            } catch (e: Exception) {
                logger.error("Error processing file URL: $url", e)
                url // Return original URL if processing fails
            }
        } else {
            url
        }
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
                    val tempFile = Files.createTempFile(oscalDir, "upload", ".tmp")

                    val tempFilePath = tempFile.toAbsolutePath()
                    logger.info("Created temporary file: $tempFilePath")
    
                    // Write the body content to the temporary file
                    tempFile.appendText(body)
                    logger.info("Wrote body content to temporary file")
    
                    // Use async for parallelism
                    val result = async {
                        executeCommand(listOf("validate", tempFilePath.toString(),"--show-stack-trace"))
                    }.await() // Wait for the result of the async execution
                    
                    logger.info("Validation result: ${result.second}")
                    if(result.first.exitCode.toString()==="OK"){
                        sendSuccessResponse(ctx, result.first, result.second)
                    }else{
                        sendErrorResponse(ctx, 400, result.first.exitCode.toString())
                    }
                    
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
                val encodedContent = ctx.queryParam("document").firstOrNull()
                val constraint = ctx.queryParam("constraint")
                val flags = ctx.queryParam("flags")
                if (encodedContent != null) {
                    val content = processUrl(encodedContent)
                    val args = mutableListOf("validate", content, "--show-stack-trace")
                    constraint.forEach { constraint_document ->
                        args.add("-c")
                        args.add(processUrl(constraint_document))
                    }
                    flags.forEach { flag ->
                        args.add(flagToParam(flag))
                    }
                    val result = async {
                        try {
                            executeCommand(args)
                        } catch (e: Exception) {
                            logger.error("Error handling request", e)
                            executeCommand(args)
                        }
                    }.await()
                    logger.info(result.second)
                    sendSuccessResponse(ctx, result.first, result.second)
                } else {
                    sendErrorResponse(ctx, 400, "content parameter is missing")
                }
            } catch (e: Exception) {
                logger.error("Error handling request", e)
                sendErrorResponse(ctx, 500, "Internal server error")
            }
        }
    }
    private fun handleResolveRequest(ctx: RoutingContext) {
        launch {
            try {
            val encodedContent = ctx.queryParam("document").firstOrNull()
                if (encodedContent != null) {
                    val content = processUrl(encodedContent)
                    val acceptHeader = ctx.request().getHeader("Accept")
                    var formatParam = ctx.queryParam("format").firstOrNull()
                    val format = mapMimeTypeToFormat(acceptHeader,formatParam)
                    // Use async for parallelism
                    val result = async {
                        executeCommand(parseCommandToArgs("resolve-profile $content --to=$format"))
                    }.await() // Wait for the result of the async execution
                    logger.info(result.second)
                    ctx.response().putHeader("Content-Type", mapFormatToMimeType(format))
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
        private fun mapMimeTypeToFormat(mimeType: String?, formatParam: String?): String {
            // Check if a valid format parameter is provided
            if (!formatParam.isNullOrBlank()) {
                return when (formatParam.lowercase()) {
                    "json" -> "JSON"
                    "xml" -> "XML"
                    "yaml" -> "YAML"
                    else -> "JSON" // Default to JSON if an invalid format is provided
                }
            }

            // If no valid format parameter, check the MIME type
            mimeType?.lowercase()?.let {
                return when {
                    it.contains("json") -> "JSON"
                    it.contains("xml") -> "XML"
                    it.contains("yaml") -> "YAML"
                    else -> "JSON" // Default to JSON if no valid MIME type is provided
                }
            }

            // Default to JSON if neither a valid formatParam nor MIME type is provided
            return "JSON"
        }


        private fun mapFormatToMimeType(format: String?): String {
            return when (format) {
                "JSON"->"application/json"
                "XML"->"text/xml"
                "YAML" -> "text/yaml"
                else -> "application/json" // Default to JSON if no valid MIME type is provided
            }
        }
         private fun flagToParam(format: String): String {
            return when (format) {
                "disable-schema"->"--disable-schema-validation"
                "disable-constraint"->"--disable-constraint-validation"
                else -> "--quiet" // Default to JSON if no valid MIME type is provided
            }
        }
        private fun handleConvertRequest(ctx: RoutingContext) {
        launch {
            try {
            val encodedContent = ctx.queryParam("document").firstOrNull()
                if (encodedContent != null) {
                    val content = processUrl(encodedContent)
                    val acceptHeader = ctx.request().getHeader("Accept")
                    var formatParam = ctx.queryParam("format").firstOrNull()
                    val format = mapMimeTypeToFormat(acceptHeader,formatParam)

                    val result = async {
                        executeCommand(parseCommandToArgs("convert $content --to=$format"))
                    }.await() // Wait for the result of the async execution
                    logger.info(result.second)
                    ctx.response().putHeader("Content-Type", mapFormatToMimeType(format))
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
        activeWorkers.incrementAndGet()
        return withContext(vertx.dispatcher()) {
            awaitBlocking {

                val command = args[0]
                                
                // Create a mutable list from args
                val mutableArgs = args.toMutableList()
        
                // Generate SARIF file name and path
                val guid = UUID.randomUUID().toString()
                val sarifFileName = "${guid}.sarif"
                val sarifFilePath = oscalDir.resolve(sarifFileName).toString()
                                
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
                activeWorkers.decrementAndGet()
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
        File(sarifFilePath).delete()
        ctx.response()
            .setStatusCode(200) // HTTP 200 OK
            .putHeader("Exit-Status", exitStatus.exitCode.toString())
            .end(fileContent)
    }

    private fun sendErrorResponse(ctx: RoutingContext, exitCode: Int, message: String) {
        ctx.response()
            .setStatusCode(exitCode)
            .putHeader("Exit-Status", "PROCESSING_ERROR")
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
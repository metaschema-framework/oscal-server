/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server
import gov.nist.secauto.oscal.tools.server.commands.OscalCommandExecutor
import gov.nist.secauto.metaschema.databind.io.IBoundLoader;
import gov.nist.secauto.oscal.tools.cli.core.OscalCliVersion;
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions;
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.UUID
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.BodyHandler
import gov.nist.secauto.metaschema.cli.processor.ExitCode
import gov.nist.secauto.metaschema.cli.processor.ExitCode.RUNTIME_ERROR
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
import gov.nist.secauto.oscal.tools.cli.core.CLI;

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
        routerBuilder.operation("resolveUpload").handler { ctx -> handleResolveFileUpload(ctx) }
        routerBuilder.operation("convert").handler { ctx -> handleConvertRequest(ctx) }
        routerBuilder.operation("convertUpload").handler { ctx -> handleConvertFileUpload(ctx) }
        routerBuilder.operation("query").handler { ctx -> handleQueryRequest(ctx) }
        routerBuilder.operation("queryUpload").handler { ctx -> handleQueryFileUpload(ctx) }
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

    private fun handleQueryRequest(ctx: RoutingContext) {
        launch {
            try {
                logger.info("Handling Query request")
                val encodedContent = ctx.queryParam("document").firstOrNull()
                val expression = ctx.queryParam("expression").firstOrNull()
                if (encodedContent != null && expression != null) {
                    val content = processUrl(encodedContent)
                    val args = mutableListOf("query")
                    args.add("-i")
                    args.add(content)
                    args.add("-e")
                    args.add(expression)
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

    

    private fun processUrl(url: String): String {
        if (!url.startsWith("file://")) {
            return url
        }

        try {
            // Remove the "file://" prefix and decode the URL
            val decodedPath = URLDecoder.decode(url.substring(7), StandardCharsets.UTF_8.name())
            
            val result = when {
                System.getProperty("os.name").lowercase().contains("win") -> {
                    // Windows-specific handling
                    if (decodedPath.startsWith("/")) {
                        // Absolute path with drive letter
                        decodedPath.substring(1).replace('/', '\\')
                    } else {
                        // UNC path or relative path
                        decodedPath.replace('/', '\\')
                    }
                }
                else -> {
                    // Unix-like systems
                    decodedPath
                }
            }            
            return result
        } catch (e: Exception) {
            return url
        }
    }
    
    
    private fun handleValidateFileUpload(ctx: RoutingContext) {
        logger.info("Handling file upload request!")
        launch {
            try {
                logger.info("Handling file upload request in the background")
                val body = ctx.body().asString()
                logger.info("Received body: $body")
                val flags = ctx.queryParam("flags")
                val encodedModule = ctx.queryParam("module").firstOrNull()

                if (body.isNotEmpty()) {
                    // Create a temporary file
                    val tempFile = Files.createTempFile(oscalDir, "upload", ".tmp")

                    val tempFilePath = tempFile.toAbsolutePath()
                    logger.info("Created temporary file: $tempFilePath")
                    val args = mutableListOf("validate");
                    encodedModule?.let { module ->
                        if (module == "http://csrc.nist.gov/ns/oscal/metaschema/1.0") {
                            args[0]="metaschema"
                            args.add("validate")
                        }else{
                            args[0]="metaschema"
                            args.add("validate-content")
                        }
                    }
                    args.add(tempFilePath.toString());
                    args.add("--show-stack-trace");
                    flags.forEach { flag ->
                        args.add(flagToParam(flag))
                    }    
                    // Write the body content to the temporary file
                    tempFile.appendText(body)
                    logger.info("Wrote body content to temporary file")
    
                    // Use async for parallelism
                    val result = async {
                        executeCommand(args)
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
            val options = HttpServerOptions()
                .setHost("localhost")  // This restricts the server to localhost
                .setPort(8888);        // You can change this port as needed
            
            val server = vertx.createHttpServer(options)
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
                val encodedModule = ctx.queryParam("module").firstOrNull()
                val constraint = ctx.queryParam("constraint")
                val flags = ctx.queryParam("flags")
                if (encodedContent != null) {
                    val content = processUrl(encodedContent)
                    val args = mutableListOf("validate");
                    encodedModule?.let { module ->
                        if (module == "http://csrc.nist.gov/ns/oscal/metaschema/1.0") {
                            args[0]="metaschema"
                            args.add("validate")
                        }else{
                            args[0]="metaschema"
                            args.add("validate-content")
                        }
                    }
                    args.add(content);
                    args.add("--show-stack-trace");
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
                else -> "ERROR" 
            }
        }

        // If no valid format parameter, check the MIME type
        mimeType?.lowercase()?.let {
            return when {
                it.contains("json") -> "JSON"
                it.contains("xml") -> "XML"
                it.contains("yaml") -> "YAML"
                else -> "ERROR" // Default to JSON if no valid MIME type is provided
            }
        }

        // Default to JSON if neither a valid formatParam nor MIME type is provided
        return "ERROR"
    }


    private fun mapFormatToMimeType(format: String?): String {
        return when (format) {
            "JSON"->"application/json"
            "XML"->"text/xml"
            "YAML" -> "text/yaml"
            else -> "ERROR" // Default to JSON if no valid MIME type is provided
        }
    }
     private fun flagToParam(format: String): String {
        return when (format) {
            "disable-schema"->"--disable-schema-validation"
            "disable-constraint"->"--disable-constraint-validation"
            else -> "--quiet" 
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
                if (listOf("metaschema","validate","query").contains(mutableArgs[0])){
                    if(!mutableArgs.contains(("--sarif-include-pass"))&&mutableArgs[0]!="query"){
                        mutableArgs.add("--sarif-include-pass")
                    }
                    mutableArgs.add("-o")    
                }

                mutableArgs.add(sarifFilePath)

                logger.info(mutableArgs.joinToString(" "))
                
                val exitStatus = try {
                    if(mutableArgs[0]=="query"){
                        logger.info(mutableArgs.joinToString(" "))
                        val oscalCommandExecutor = OscalCommandExecutor(mutableArgs[0], mutableArgs)
                        oscalCommandExecutor.executeCommand()
                    }else{
                        CLI.runCli(*mutableArgs.toTypedArray())
                    }
                } catch (e: Exception) {
                    MessageExitStatus(ExitCode.RUNTIME_ERROR, e.message)
                }
                // Check if SARIF file was created
                if (!File(sarifFilePath).exists()) {
                    val exitCode = "code:${exitStatus.exitCode}"
                    val basicSarif = when (exitStatus) {
                        is MessageExitStatus -> {
                            // Always create SARIF, but include message if available
                            val message = exitStatus.message
                            if (!message.isNullOrEmpty()) {
                                createBasicSarif(exitCode+" "+ message)
                            } else {
                                createBasicSarif(exitCode)
                            }
                        }
                        else -> createBasicSarif(exitCode)
                    }
                    
                    File(sarifFilePath).writeText(basicSarif)
                }
                activeWorkers.decrementAndGet()
                Pair(exitStatus, sarifFilePath)
            }
        }
    }

    private fun createBasicSarif(errorMessage: String): String {
        val version = OscalCliVersion().getVersion();
        return """
        {
          "${'$'}schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
          "version": "2.1.0",
          "runs": [
            {
              "tool": {
                "driver": {
                  "name": "OSCAL Server",
                  "informationUri": "https://github.com/metaschema-framework/oscal-server",
                  "version": "$version"
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
    private fun handleQueryFileUpload(ctx: RoutingContext) {
    launch {
        try {
            logger.info("Handling Query file upload request")
            val body = ctx.body().asString()
            val expression = ctx.queryParam("expression").firstOrNull()

            if (body.isNotEmpty() && expression != null) {
                // Create a temporary file
                val tempFile = Files.createTempFile(oscalDir, "upload", ".tmp")
                val tempFilePath = tempFile.toAbsolutePath()
                logger.info("Created temporary file: $tempFilePath")

                // Write the body content to the temporary file
                tempFile.appendText(body)
                logger.info("Wrote body content to temporary file")

                val args = mutableListOf("query")
                args.add("-i")
                args.add(processUrl(tempFilePath.toString()))
                args.add("-e")
                args.add(expression)

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
                
                // Clean up temporary file
            } else {
                sendErrorResponse(ctx, 400, "Missing request body or expression parameter")
            }
        } catch (e: Exception) {
            logger.error("Error handling file upload request", e)
            sendErrorResponse(ctx, 500, "Internal server error")
        }
    }
}

private fun handleResolveFileUpload(ctx: RoutingContext) {
    launch {
        try {
            logger.info("Handling Resolve file upload request")
            val body = ctx.body().asString()
            val acceptHeader = ctx.request().getHeader("Accept")
            val formatParam = ctx.queryParam("format").firstOrNull()
            val format = mapMimeTypeToFormat(acceptHeader, formatParam)

            if (body.isNotEmpty()) {
                // Create a temporary file
                val tempFile = Files.createTempFile(oscalDir, "upload", ".tmp")
                val tempFilePath = tempFile.toAbsolutePath()
                logger.info("Created temporary file: $tempFilePath")

                // Write the body content to the temporary file
                tempFile.appendText(body)
                logger.info("Wrote body content to temporary file")

                val result = async {
                    executeCommand(parseCommandToArgs("resolve-profile ${processUrl(tempFilePath.toString())} --to=$format"))
                }.await()
                
                logger.info(result.second)
                ctx.response().putHeader("Content-Type", mapFormatToMimeType(format))
                sendSuccessResponse(ctx, result.first, result.second)
                
                // Clean up temporary file
            } else {
                sendErrorResponse(ctx, 400, "No content in request body")
            }
        } catch (e: Exception) {
            logger.error("Error handling file upload request", e)
            sendErrorResponse(ctx, 500, "Internal server error")
        }
    }
}

private fun handleConvertFileUpload(ctx: RoutingContext) {
    launch {
        try {
            logger.info("Handling Convert file upload request")
            val body = ctx.body().asString()
            val acceptHeader = ctx.request().getHeader("Accept")
            val formatParam = ctx.queryParam("format").firstOrNull()
            val format = mapMimeTypeToFormat(acceptHeader, formatParam)

            if (body.isNotEmpty()) {
                // Create a temporary file
                val tempFile = Files.createTempFile(oscalDir, "upload", ".tmp")
                val tempFilePath = tempFile.toAbsolutePath()
                logger.info("Created temporary file: $tempFilePath")

                // Write the body content to the temporary file
                tempFile.appendText(body)
                logger.info("Wrote body content to temporary file")

                val result = async {
                    executeCommand(parseCommandToArgs("convert ${processUrl(tempFilePath.toString())} --to=$format"))
                }.await()
                
                logger.info(result.second)
                ctx.response().putHeader("Content-Type", mapFormatToMimeType(format))
                sendSuccessResponse(ctx, result.first, result.second)
                
                // Clean up temporary file
            } else {
                sendErrorResponse(ctx, 400, "No content in request body")
            }
        } catch (e: Exception) {
            logger.error("Error handling file upload request", e)
            sendErrorResponse(ctx, 500, "Internal server error")
        }
    }
}
}

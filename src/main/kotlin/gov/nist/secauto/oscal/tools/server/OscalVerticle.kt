/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server
import java.nio.file.attribute.PosixFilePermission
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
    private lateinit var serverDir: Path
    private val allowedDirs = mutableListOf<Path>()

    private val activeWorkers = AtomicInteger(0)

    
    override suspend fun start() {
        try {
            VertxOptions().setEventLoopPoolSize(8)
            validateAndInitializeDirectories()
            serverDir = Paths.get("").toAbsolutePath()
            val router = createRouter()
            startHttpServer(router)
        } catch (e: SecurityException) {
            logger.error("Critical security configuration error: ${e.message}")
            throw e // Fail fast on security configuration issues
        }
    }

    private fun validateAndInitializeDirectories() {
        // Initialize OSCAL directory with security checks
        initializeOscalDirectory()
        
        // Initialize and validate allowed directories
        initializeAllowedDirectories()
        
        // Verify all directories are accessible and have proper permissions
        verifyDirectoryPermissions()
    }

    private fun initializeOscalDirectory() {
        val homeDir = System.getProperty("user.home")
        oscalDir = Paths.get(homeDir, ".oscal").normalize().toAbsolutePath()
        
        if (!Files.exists(oscalDir)) {
            try {
                // Create directory with restricted permissions (owner read/write/execute only)
                Files.createDirectory(oscalDir)
                restrictDirectoryPermissions(oscalDir)
            } catch (e: Exception) {
                throw SecurityException("Failed to create secure OSCAL directory: ${e.message}")
            }
        }
        
        if (!Files.isDirectory(oscalDir)) {
            throw SecurityException("OSCAL path exists but is not a directory: $oscalDir")
        }
        
        logger.info("OSCAL directory initialized at: $oscalDir")
    }

    private fun initializeAllowedDirectories() {
        // Clear existing allowed directories
        allowedDirs.clear()
        
        // Always add ~/.oscal as the first allowed directory
        allowedDirs.add(oscalDir)

        val envPath = System.getenv("OSCAL_SERVER_PATH")
        if (!envPath.isNullOrBlank()) {
            val paths = envPath.split(File.pathSeparator)
            for (dir in paths) {
                try {
                    val expandedDir = expandHomeDirectory(dir.trim())
                    val path = Paths.get(expandedDir).normalize().toAbsolutePath()
                    
                    // Validate the directory
                    validateDirectory(path)
                    
                    // Add to allowed directories if validation passes
                    allowedDirs.add(path)
                    logger.info("Added allowed directory from OSCAL_SERVER_PATH: $path")
                } catch (e: Exception) {
                    logger.error("Invalid directory in OSCAL_SERVER_PATH: $dir - ${e.message}")
                    throw SecurityException("Invalid directory configuration: $dir - ${e.message}")
                }
            }
        } else {
            logger.warn("OSCAL_SERVER_PATH environment variable not set - only ~/.oscal will be accessible")
        }

        if (allowedDirs.isEmpty()) {
            throw SecurityException("No valid directories configured for access")
        }

        logger.info("Initialized allowed directories: ${allowedDirs.joinToString(", ")}")
    }

    private fun validateDirectory(path: Path) {
        when {
            !Files.exists(path) -> 
                throw SecurityException("Directory does not exist: $path")
            !Files.isDirectory(path) -> 
                throw SecurityException("Path is not a directory: $path")
            !Files.isReadable(path) -> 
                throw SecurityException("Directory is not readable: $path")
            path.startsWith(oscalDir) && !path.equals(oscalDir) -> 
                throw SecurityException("Security violation: Subdirectories of ~/.oscal are not allowed")
        }
    }

    private fun verifyDirectoryPermissions() {
        allowedDirs.forEach { dir ->
            try {
                // Verify basic access permissions
                require(Files.isReadable(dir)) { "Directory not readable: $dir" }
                require(Files.isExecutable(dir)) { "Directory not executable: $dir" }
                
                // Check for suspicious symlinks
                if (Files.isSymbolicLink(dir)) {
                    val target = Files.readSymbolicLink(dir)
                    val resolvedTarget = dir.resolveSibling(target).normalize()
                    require(allowedDirs.any { allowed -> resolvedTarget.startsWith(allowed) }) {
                        "Symbolic link points outside allowed directories: $dir -> $resolvedTarget"
                    }
                }
            } catch (e: Exception) {
                throw SecurityException("Directory permission verification failed for $dir: ${e.message}")
            }
        }
    }

    private fun restrictDirectoryPermissions(path: Path) {
        try {
            // Set directory permissions to owner read/write/execute only
            val perms = Files.getPosixFilePermissions(path)
            perms.removeAll(setOf(
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_WRITE,
                PosixFilePermission.OTHERS_EXECUTE
            ))
            Files.setPosixFilePermissions(path, perms)
        } catch (e: Exception) {
            logger.warn("Failed to restrict directory permissions: ${e.message}")
            // Continue execution but log the warning
        }
    }

    private fun processUrl(url: String): String {
        return when {
            url.startsWith("https://") -> {
                // HTTPS URLs are allowed as-is
                url
            }
            url.startsWith("file://") -> {
                processFileUrl(url)
            }
            else -> {
                logger.error("Invalid URL scheme: $url")
                throw SecurityException("Only https:// URLs or allowed local files are permitted.")
            }
        }
    }

    private fun processFileUrl(url: String): String {
        try {
            val decodedPath = URLDecoder.decode(url.substring(7), StandardCharsets.UTF_8.name())
            val normalizedPath = if (System.getProperty("os.name").lowercase().contains("win")) {
                // Windows-specific handling
                val winPath = if (decodedPath.startsWith("/")) {
                    decodedPath.substring(1).replace('/', '\\')
                } else {
                    decodedPath.replace('/', '\\')
                }
                Paths.get(winPath).normalize().toAbsolutePath()
            } else {
                Paths.get(decodedPath).normalize().toAbsolutePath()
            }

            // Check for directory traversal attempts
            val canonicalPath = normalizedPath.toFile().canonicalPath
            if (canonicalPath != normalizedPath.toString()) {
                throw SecurityException("Potential directory traversal detected")
            }

            // Verify path is under allowed directories
            if (!allowedDirs.any { allowedDir -> normalizedPath.startsWith(allowedDir) }) {
                throw SecurityException("Access denied: File is not in an allowed directory: $normalizedPath")
            }

            return normalizedPath.toString()
        } catch (e: Exception) {
            logger.error("Error processing file URL: $url", e)
            throw SecurityException("Invalid file URL: ${e.message}")
        }
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
                val module = ctx.queryParam("module").firstOrNull()
                if (encodedContent != null && expression != null && module != null) {
                    val content = processUrl(encodedContent)
                    val args = mutableListOf("metaschema","metapath","eval")
                    args.add("-i")
                    args.add(content)
                    args.add("-e")
                    args.add(expression)
                    args.add("-m")
                    args.add(module)
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

    private fun expandHomeDirectory(path: String): String {
        return if (path.startsWith("~")) {
            val home = System.getProperty("user.home")
            home + path.substring(1)
        } else {
            path
        }
    }

    

    private fun unescapeXmlString(xml: String): String {
        return xml.replace("\\\"", "\"")  // Replace escaped quotes with regular quotes
                 .replace("\\'", "'")      // Replace escaped single quotes
                 .replace("\\n", "\n")     // Replace escaped newlines
                 .replace("\\r", "\r")     // Replace escaped carriage returns
                 .replace("\\t", "\t")     // Replace escaped tabs
    }
    
    private fun handleValidateFileUpload(ctx: RoutingContext) {
        logger.info("Handling file upload request!")
        launch {
            try {
                logger.info("Handling file upload request in the background")
                var body = ctx.body().asString()
                    // Remove surrounding quotes if they exist
                if (body.startsWith("\"") && body.endsWith("\"")) {
                    body = body.substring(1, body.length - 1)
                }
                if (body.trim().startsWith("<")) {
                    body = unescapeXmlString(body)
                }
                logger.info("Received body: $body")
                val flags = ctx.queryParam("flags")
                val encodedModule = ctx.queryParam("module").firstOrNull()
                
                // Get the format parameter if provided
                val formatParam = ctx.queryParam("format").firstOrNull()?.lowercase()
                val fileExtension = when (formatParam) {
                    "json" -> ".json"
                    "xml" -> ".xml"
                    "yaml" -> ".yaml"
                    else -> ".tmp"
                }
    
                if (body.isNotEmpty()) {
                    // Create a temporary file with the chosen extension
                    val tempFile = Files.createTempFile(oscalDir, "upload", fileExtension)
                    val tempFilePath = tempFile.toAbsolutePath()
                    logger.info("Created temporary file: $tempFilePath")
    
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
                    args.add(tempFilePath.toString())
                    args.add("--show-stack-trace")
                    flags.forEach { flag ->
                        args.add(flagToParam(flag))
                    }
    
                    // Write the body content to the temporary file
                    tempFile.appendText(body)
                    logger.info("Wrote body content to temporary file")
    
                    // Use async for parallel execution
                    val result = async {
                        executeCommand(args)
                    }.await()
    
                    logger.info("Validation result: ${result.second}")
    
                    if (result.first.exitCode.toString() == "OK") {
                        sendSuccessResponse(ctx, result.first, result.second)
                    } else {
                        sendErrorResponse(ctx, 400, result.first.exitCode.toString())
                    }
                    // Temporary file may be cleaned up later if desired
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
                val isQuery=mutableArgs[1]=="metapath";
                if (listOf("metaschema","validate").contains(mutableArgs[0])&&!isQuery){
                    mutableArgs.add("--sarif-include-pass")
                    mutableArgs.add("-o")    
                }
                if(!isQuery){
                    mutableArgs.add(sarifFilePath)
                }

                logger.info(mutableArgs.joinToString(" "))
                
                val exitStatus = try {
                        CLI.runCli(*mutableArgs.toTypedArray())
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
            val module = ctx.queryParam("module").firstOrNull()

            if (body.isNotEmpty() && expression != null&&module!=null) {
                // Create a temporary file
                val tempFile = Files.createTempFile(oscalDir, "upload", ".tmp")
                val tempFilePath = tempFile.toAbsolutePath()
                logger.info("Created temporary file: $tempFilePath")

                // Write the body content to the temporary file
                tempFile.appendText(body)
                logger.info("Wrote body content to temporary file")

                val args = mutableListOf("metaschema","metapath","eval")
                args.add("-i")
                args.add(processUrl(tempFilePath.toString()))
                args.add("-e")
                args.add(expression)
                args.add("-m")
                args.add(module)

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

/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.openapi.OpenAPILoaderOptions
import io.vertx.ext.web.openapi.RouterBuilder
import io.vertx.core.VertxOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager
import java.nio.file.Path
import java.nio.file.Paths
import io.vertx.core.Promise
import io.vertx.core.DeploymentOptions

class OscalVerticle : CoroutineVerticle() {
    private val logger: Logger = LogManager.getLogger(OscalVerticle::class.java)
    private lateinit var directoryManager: DirectoryManager
    private lateinit var urlProcessor: UrlProcessor
    private lateinit var commandExecutor: CommandExecutor
    private lateinit var responseHandler: ResponseHandler
    private lateinit var requestHandler: RequestHandler
    private lateinit var packageHandler: PackageHandler
    private lateinit var moduleHandler: ModuleHandler
    private lateinit var serverDir: Path
    private lateinit var packagesDir: Path
    private lateinit var uploadsDir: Path
    
    override suspend fun start() {
        try {
            logger.info("Starting OscalVerticle...")
            initializeComponents()
            serverDir = Paths.get("").toAbsolutePath()
            logger.info("Creating router...")
            val router = createRouter()
            logger.info("Starting HTTP server...")
            startHttpServer(router)
        } catch (e: SecurityException) {
            logger.error("Critical security configuration error: ${e.message}")
            throw e // Fail fast on security configuration issues
        } catch (e: Exception) {
            logger.error("Failed to start OscalVerticle", e)
            throw e
        }
    }

    private fun initializeComponents() {
        logger.info("Initializing components...")
        directoryManager = DirectoryManager()
        val oscalDir = directoryManager.initialize()
        packagesDir = oscalDir.resolve("packages")
        uploadsDir = oscalDir.resolve("uploads")
        urlProcessor = UrlProcessor(directoryManager.getAllowedDirs())
        commandExecutor = CommandExecutor(oscalDir)
        responseHandler = ResponseHandler()
        requestHandler = RequestHandler(urlProcessor, commandExecutor, responseHandler, oscalDir)
        packageHandler = PackageHandler(packagesDir)
        moduleHandler = ModuleHandler(packagesDir)
        logger.info("Components initialized successfully")
    }

    private suspend fun createRouter(): Router {
        logger.info("Creating router")
        val options = OpenAPILoaderOptions()
        val routerBuilder = RouterBuilder.create(vertx, "webroot/openapi.yaml", options).coAwait()
        logger.info("Router builder created")

        // Configure base BodyHandler for JSON/YAML/XML content
        val bodyHandler = BodyHandler.create()
            .setBodyLimit(1000000000) // 1GB max body size
            .setPreallocateBodyBuffer(false)

        // Add body handler before any other handlers
        routerBuilder.bodyHandler(bodyHandler)

        // Add logging handlers
        routerBuilder.rootHandler { ctx ->
            logger.info("Request handling: ${ctx.request().method()} ${ctx.request().uri()}")
            ctx.next()
        }

        // Configure upload operations with direct body content
        routerBuilder.operation("validateUpload").handler { ctx -> 
            launch(vertx.dispatcher()) {
                requestHandler.handleValidateFileUpload(ctx)
            }
        }
        routerBuilder.operation("resolveUpload").handler { ctx -> 
            launch(vertx.dispatcher()) {
                requestHandler.handleResolveFileUpload(ctx)
            }
        }
        routerBuilder.operation("convertUpload").handler { ctx -> 
            launch(vertx.dispatcher()) {
                requestHandler.handleConvertFileUpload(ctx)
            }
        }
        routerBuilder.operation("queryUpload").handler { ctx -> 
            launch(vertx.dispatcher()) {
                requestHandler.handleQueryFileUpload(ctx)
            }
        }
        
        // Handle regular operations with suspend functions
        routerBuilder.operation("validate").handler { ctx ->
            launch(vertx.dispatcher()) {
                requestHandler.handleValidateRequest(ctx)
            }
        }
        routerBuilder.operation("resolve").handler { ctx ->
            launch(vertx.dispatcher()) {
                requestHandler.handleResolveRequest(ctx)
            }
        }
        routerBuilder.operation("convert").handler { ctx ->
            launch(vertx.dispatcher()) {
                requestHandler.handleConvertRequest(ctx)
            }
        }
        routerBuilder.operation("query").handler { ctx ->
            launch(vertx.dispatcher()) {
                requestHandler.handleQueryRequest(ctx)
            }
        }

        // Handle package operations
        routerBuilder.operation("listPackageFiles").handler { ctx -> packageHandler.handleListPackageFiles(ctx) }
        routerBuilder.operation("uploadPackageFile").handler { ctx -> packageHandler.handleUploadPackageFile(ctx) }
        routerBuilder.operation("getPackageFile").handler { ctx -> packageHandler.handleGetPackageFile(ctx) }
        routerBuilder.operation("updatePackageFile").handler { ctx -> packageHandler.handleUpdatePackageFile(ctx) }
        routerBuilder.operation("deletePackageFile").handler { ctx -> packageHandler.handleDeletePackageFile(ctx) }

        // Handle module operations
        routerBuilder.operation("listModuleFiles").handler { ctx -> moduleHandler.handleListFiles(ctx) }
        routerBuilder.operation("uploadModuleFile").handler { ctx -> moduleHandler.handleUploadFile(ctx) }
        routerBuilder.operation("getModuleFile").handler { ctx -> moduleHandler.handleGetFile(ctx) }
        routerBuilder.operation("updateModuleFile").handler { ctx -> moduleHandler.handleUpdateFile(ctx) }
        routerBuilder.operation("deleteModuleFile").handler { ctx -> moduleHandler.handleDeleteFile(ctx) }
        
        // Handle health check
        routerBuilder.operation("healthCheck").handler { ctx -> requestHandler.handleHealthCheck(ctx) }

        // Create the router
        val router = routerBuilder.createRouter()

        // Configure body handler for direct routes
        val directRouteBodyHandler = BodyHandler.create()
            .setBodyLimit(1000000000) // 1GB max body size
            .setPreallocateBodyBuffer(false)

        // Add direct route handlers for legacy support
        router.get("/health").handler { ctx -> requestHandler.handleHealthCheck(ctx) }
        
        // Validate routes
        router.get("/validate").handler { ctx ->
            launch(vertx.dispatcher()) {
                requestHandler.handleValidateRequest(ctx)
            }
        }
        router.post("/validate")
            .handler(directRouteBodyHandler)
            .handler { ctx ->
                launch(vertx.dispatcher()) {
                    requestHandler.handleValidateFileUpload(ctx)
                }
            }
            
        // Convert routes
        router.get("/convert").handler { ctx ->
            launch(vertx.dispatcher()) {
                requestHandler.handleConvertRequest(ctx)
            }
        }
        router.post("/convert")
            .handler(directRouteBodyHandler)
            .handler { ctx ->
                launch(vertx.dispatcher()) {
                    requestHandler.handleConvertFileUpload(ctx)
                }
            }
            
        // Resolve profile routes
        router.get("/resolve-profile").handler { ctx ->
            launch(vertx.dispatcher()) {
                requestHandler.handleResolveRequest(ctx)
            }
        }
        router.post("/resolve-profile")
            .handler(directRouteBodyHandler)
            .handler { ctx ->
                launch(vertx.dispatcher()) {
                    requestHandler.handleResolveFileUpload(ctx)
                }
            }

        // Add static file handler last
        router.route("/*").handler(StaticHandler.create("webroot"))
        logger.info("Router created successfully with both API and direct routes")
        return router
    }

    private suspend fun startHttpServer(router: Router) {
        try {
            val options = HttpServerOptions()
                .setHost("localhost")  // This restricts the server to localhost
                .setPort(8888)        // You can change this port as needed
            
            val server = vertx.createHttpServer(options)
                .requestHandler(router)
                .listen(8888)
                .coAwait()
            logger.info("HTTP server started on port ${server.actualPort()}")
        } catch (e: Exception) {
            logger.error("Failed to start HTTP server", e)
            throw e
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                // Configure Vert.x options
                val vertxOptions = VertxOptions()
                    .setEventLoopPoolSize(8)
                    .setWorkerPoolSize(50)
                    .setInternalBlockingPoolSize(50)
                    .setBlockedThreadCheckInterval(1000)
                    .setMaxEventLoopExecuteTime(2000000000) // 2 seconds in nanoseconds

                // Create Vert.x instance with options
                val vertx = Vertx.vertx(vertxOptions)
                
                // Configure deployment options
                val deploymentOptions = DeploymentOptions()
                    .setInstances(1)
                    // Using non-deprecated method
                    .setDeploymentWorker(false)

                // Deploy verticle and handle result
                vertx.deployVerticle(OscalVerticle(), deploymentOptions) { ar ->
                    if (ar.succeeded()) {
                        println("OscalVerticle deployed successfully with ID: ${ar.result()}")
                    } else {
                        println("Failed to deploy OscalVerticle: ${ar.cause()}")
                        ar.cause().printStackTrace()
                        // Shutdown Vert.x on deployment failure
                        vertx.close()
                    }
                }
            } catch (e: Exception) {
                println("Error during Vert.x initialization: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}

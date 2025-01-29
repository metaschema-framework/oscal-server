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

class OscalVerticle : CoroutineVerticle() {
    private val logger: Logger = LogManager.getLogger(OscalVerticle::class.java)
    private lateinit var directoryManager: DirectoryManager
    private lateinit var urlProcessor: UrlProcessor
    private lateinit var commandExecutor: CommandExecutor
    private lateinit var responseHandler: ResponseHandler
    private lateinit var requestHandler: RequestHandler
    private lateinit var serverDir: Path
    
    override suspend fun start() {
        try {
            VertxOptions().setEventLoopPoolSize(8)
            initializeComponents()
            serverDir = Paths.get("").toAbsolutePath()
            val router = createRouter()
            startHttpServer(router)
        } catch (e: SecurityException) {
            logger.error("Critical security configuration error: ${e.message}")
            throw e // Fail fast on security configuration issues
        }
    }

    private fun initializeComponents() {
        directoryManager = DirectoryManager()
        val oscalDir = directoryManager.initialize()
        urlProcessor = UrlProcessor(directoryManager.getAllowedDirs())
        commandExecutor = CommandExecutor(oscalDir)
        responseHandler = ResponseHandler()
        requestHandler = RequestHandler(urlProcessor, commandExecutor, responseHandler, oscalDir)
    }

    private suspend fun createRouter(): Router {
        logger.info("Creating router")
        val options = OpenAPILoaderOptions()
        val routerBuilder = RouterBuilder.create(vertx, "webroot/openapi.yaml", options).coAwait()
        logger.info("Router builder created")

        // Add BodyHandler to handle file uploads
        routerBuilder.rootHandler(BodyHandler.create("webroot"))
        
        // Handle file upload operations
        routerBuilder.operation("validateUpload").handler { ctx -> requestHandler.handleValidateFileUpload(ctx) }
        routerBuilder.operation("resolveUpload").handler { ctx -> requestHandler.handleResolveFileUpload(ctx) }
        routerBuilder.operation("convertUpload").handler { ctx -> requestHandler.handleConvertFileUpload(ctx) }
        routerBuilder.operation("queryUpload").handler { ctx -> requestHandler.handleQueryFileUpload(ctx) }
        
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
        
        // Handle health check
        routerBuilder.operation("healthCheck").handler { ctx -> requestHandler.handleHealthCheck(ctx) }

        val router = routerBuilder.createRouter()
        router.route("/*").handler(StaticHandler.create("webroot"))
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
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val vertx = Vertx.vertx()
            vertx.deployVerticle(OscalVerticle())
        }
    }
}

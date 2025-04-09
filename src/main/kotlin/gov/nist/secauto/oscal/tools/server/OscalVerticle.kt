package gov.nist.secauto.oscal.tools.server

import gov.nist.secauto.oscal.tools.server.mcp.OscalMcpServer
import gov.nist.secauto.oscal.tools.server.mcp.ServerTransport
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch

class OscalVerticle : CoroutineVerticle() {
    private lateinit var documentValidator: DocumentValidator
    private lateinit var documentConverter: DocumentConverter
    private lateinit var profileResolver: ProfileResolver
    private lateinit var packageHandler: PackageHandler
    private lateinit var mcpServer: OscalMcpServer

    override suspend fun start() {
        // Initialize handlers
        documentValidator = DocumentValidator()
        documentConverter = DocumentConverter()
        profileResolver = ProfileResolver()
        packageHandler = PackageHandler()

        // Initialize MCP server
        mcpServer = OscalMcpServer(
            documentValidator,
            documentConverter,
            profileResolver,
            packageHandler
        )

        // Create router
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())

        // Setup HTTP endpoints
        setupHttpEndpoints(router)

        // Start HTTP server
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080)
            .onSuccess {
                println("OSCAL Server listening on port 8080")
            }
            .onFailure {
                println("Failed to start server: ${it.message}")
            }

        // Start MCP server
        launch {
            mcpServer.connect(object : ServerTransport {
                override suspend fun send(message: String) {
                    // Implementation for sending messages
                    println("MCP -> Client: $message")
                }

                override suspend fun receive(): String {
                    // Implementation for receiving messages
                    return ""
                }
            })
        }
    }

    private fun setupHttpEndpoints(router: Router) {
        // Add HTTP endpoints here if needed
        // These complement the MCP tools
        router.post("/validate").handler { ctx ->
            // Example endpoint
            ctx.response()
                .putHeader("content-type", "application/json")
                .end("{\"status\": \"ok\"}")
        }
    }

    override suspend fun stop() {
        mcpServer.close()
    }
}

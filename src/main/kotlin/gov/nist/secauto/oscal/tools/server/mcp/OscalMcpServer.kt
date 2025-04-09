package gov.nist.secauto.oscal.tools.server.mcp

import gov.nist.secauto.oscal.tools.server.*
import gov.nist.secauto.oscal.tools.server.mcp.*
import kotlinx.coroutines.runBlocking

class OscalMcpServer(
    private val documentValidator: DocumentValidator,
    private val documentConverter: DocumentConverter,
    private val profileResolver: ProfileResolver,
    private val packageHandler: PackageHandler
) {
    private val server = Server(
        ServerConfig(
            name = "oscal-server",
            version = "1.0.0",
            capabilities = ServerCapabilities(
                tools = true,
                resources = false
            )
        )
    )

    init {
        setupTools()
        
        // Error handling
        server.onerror = { error -> 
            println("[MCP Error] ${error.message}")
        }
    }

    private fun setupTools() {
        // Register tools
        server.setRequestHandler(ListToolsRequestSchema) { _ ->
            ListToolsResponse(
                tools = listOf(
                    Tool(
                        name = "validate_oscal",
                        description = "Validate an OSCAL document",
                        inputSchema = mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "document" to mapOf(
                                    "type" to "string",
                                    "description" to "OSCAL document content"
                                ),
                                "format" to mapOf(
                                    "type" to "string",
                                    "enum" to listOf("json", "xml", "yaml"),
                                    "description" to "Document format"
                                )
                            ),
                            "required" to listOf("document", "format")
                        )
                    ),
                    Tool(
                        name = "convert_oscal",
                        description = "Convert OSCAL document between formats",
                        inputSchema = mapOf(
                            "type" to "object", 
                            "properties" to mapOf(
                                "document" to mapOf(
                                    "type" to "string",
                                    "description" to "OSCAL document content"
                                ),
                                "from_format" to mapOf(
                                    "type" to "string",
                                    "enum" to listOf("json", "xml", "yaml"),
                                    "description" to "Source format"
                                ),
                                "to_format" to mapOf(
                                    "type" to "string",
                                    "enum" to listOf("json", "xml", "yaml"),
                                    "description" to "Target format"
                                )
                            ),
                            "required" to listOf("document", "from_format", "to_format")
                        )
                    ),
                    Tool(
                        name = "resolve_profile",
                        description = "Resolve an OSCAL profile",
                        inputSchema = mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "profile" to mapOf(
                                    "type" to "string",
                                    "description" to "OSCAL profile content"
                                ),
                                "format" to mapOf(
                                    "type" to "string",
                                    "enum" to listOf("json", "xml", "yaml"),
                                    "description" to "Profile format"
                                )
                            ),
                            "required" to listOf("profile", "format")
                        )
                    ),
                    Tool(
                        name = "create_package",
                        description = "Create an OSCAL package",
                        inputSchema = mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "documents" to mapOf(
                                    "type" to "array",
                                    "items" to mapOf(
                                        "type" to "object",
                                        "properties" to mapOf(
                                            "content" to mapOf(
                                                "type" to "string",
                                                "description" to "Document content"
                                            ),
                                            "format" to mapOf(
                                                "type" to "string",
                                                "enum" to listOf("json", "xml", "yaml"),
                                                "description" to "Document format"
                                            )
                                        ),
                                        "required" to listOf("content", "format")
                                    )
                                )
                            ),
                            "required" to listOf("documents")
                        )
                    )
                )
            )
        }

        server.setRequestHandler(CallToolRequestSchema) { request ->
            when (request.params.name) {
                "validate_oscal" -> handleValidateOscal(request.params.arguments)
                "convert_oscal" -> handleConvertOscal(request.params.arguments)
                "resolve_profile" -> handleResolveProfile(request.params.arguments)
                "create_package" -> handleCreatePackage(request.params.arguments)
                else -> throw McpError(ErrorCode.MethodNotFound, "Unknown tool: ${request.params.name}")
            }
        }
    }

    private fun handleValidateOscal(args: Map<String, Any>): CallToolResponse {
        try {
            val document = args["document"] as String
            val format = args["format"] as String

            val results = runBlocking {
                documentValidator.validate(document, format)
            }

            return CallToolResponse(
                content = listOf(
                    Content(
                        type = "text",
                        text = results.toString()
                    )
                )
            )
        } catch (e: Exception) {
            throw McpError(ErrorCode.InvalidParams, "Error validating document: ${e.message}")
        }
    }

    private fun handleConvertOscal(args: Map<String, Any>): CallToolResponse {
        try {
            val document = args["document"] as String
            val fromFormat = args["from_format"] as String
            val toFormat = args["to_format"] as String

            val converted = runBlocking {
                documentConverter.convert(document, fromFormat, toFormat)
            }

            return CallToolResponse(
                content = listOf(
                    Content(
                        type = "text",
                        text = converted
                    )
                )
            )
        } catch (e: Exception) {
            throw McpError(ErrorCode.InvalidParams, "Error converting document: ${e.message}")
        }
    }

    private fun handleResolveProfile(args: Map<String, Any>): CallToolResponse {
        try {
            val profile = args["profile"] as String
            val format = args["format"] as String

            val resolved = runBlocking {
                profileResolver.resolve(profile, format)
            }

            return CallToolResponse(
                content = listOf(
                    Content(
                        type = "text",
                        text = resolved
                    )
                )
            )
        } catch (e: Exception) {
            throw McpError(ErrorCode.InvalidParams, "Error resolving profile: ${e.message}")
        }
    }

    private fun handleCreatePackage(args: Map<String, Any>): CallToolResponse {
        try {
            val documents = (args["documents"] as List<Map<String, Any>>).map { doc ->
                Document(
                    content = doc["content"] as String,
                    format = doc["format"] as String
                )
            }

            val packageContent = runBlocking {
                packageHandler.createPackage(documents)
            }

            return CallToolResponse(
                content = listOf(
                    Content(
                        type = "text",
                        text = packageContent
                    )
                )
            )
        } catch (e: Exception) {
            throw McpError(ErrorCode.InvalidParams, "Error creating package: ${e.message}")
        }
    }

    fun connect(transport: ServerTransport) {
        runBlocking {
            server.connect(transport)
        }
        println("OSCAL MCP server running")
    }

    fun close() {
        runBlocking {
            server.close()
        }
    }
}

data class Document(
    val content: String,
    val format: String
)

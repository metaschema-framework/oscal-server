package gov.nist.secauto.oscal.tools.server.mcp

// Core MCP types
data class ServerConfig(
    val name: String,
    val version: String,
    val capabilities: ServerCapabilities
)

data class ServerCapabilities(
    val tools: Boolean = false,
    val resources: Boolean = false
)

data class Tool(
    val name: String,
    val description: String,
    val inputSchema: Map<String, Any>
)

data class Content(
    val type: String,
    val text: String
)

data class ListToolsResponse(
    val tools: List<Tool>
)

data class CallToolResponse(
    val content: List<Content>,
    val isError: Boolean = false
)

data class CallToolRequest(
    val params: CallToolParams
)

data class CallToolParams(
    val name: String,
    val arguments: Map<String, Any>
)

// Error handling
class McpError(val code: ErrorCode, message: String) : Exception(message) {
    companion object {
        private const val serialVersionUID = 1L
    }
}

enum class ErrorCode(val value: String) {
    InvalidRequest("invalid_request"),
    MethodNotFound("method_not_found"),
    InvalidParams("invalid_params"),
    InternalError("internal_error")
}

// Server interface
interface ServerTransport {
    suspend fun send(message: String)
    suspend fun receive(): String
}

// Request schemas
object ListToolsRequestSchema
object CallToolRequestSchema

// Server class
class Server(config: ServerConfig) {
    var onerror: ((McpError) -> Unit)? = null
    
    fun setRequestHandler(schema: Any, handler: (CallToolRequest) -> Any) {
        // Implementation would handle request routing
    }
    
    suspend fun connect(transport: ServerTransport) {
        // Implementation would handle connection setup
    }
    
    suspend fun close() {
        // Implementation would handle cleanup
    }
}

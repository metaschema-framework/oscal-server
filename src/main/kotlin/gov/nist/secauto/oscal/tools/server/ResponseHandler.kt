/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import io.vertx.ext.web.RoutingContext
import io.vertx.core.json.JsonObject
import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager

class ResponseHandler {
    private val logger: Logger = LogManager.getLogger(ResponseHandler::class.java)

    fun sendSuccessResponse(ctx: RoutingContext, exitStatus: ExitStatus, output: String) {
        try {
            when {
                ctx.response().headers().get("Content-Type")?.startsWith("application/json") == true -> {
                    // For JSON responses, try to parse and send as JSON
                    try {
                        val jsonOutput = JsonObject(output)
                        ctx.response()
                            .setStatusCode(200)
                            .putHeader("Exit-Code", exitStatus.exitCode.toString())
                            .end(jsonOutput.encode())
                    } catch (e: Exception) {
                        // If parsing fails, send as plain text
                        logger.warn("Failed to parse output as JSON, sending as text: ${e.message}")
                        ctx.response()
                            .setStatusCode(200)
                            .putHeader("Content-Type", "text/plain")
                            .putHeader("Exit-Code", exitStatus.exitCode.toString())
                            .end(output)
                    }
                }
                else -> {
                    // For non-JSON responses, send as is with the already set Content-Type
                    ctx.response()
                        .setStatusCode(200)
                        .putHeader("Exit-Code", exitStatus.exitCode.toString())
                        .end(output)
                }
            }
        } catch (e: Exception) {
            logger.error("Error sending success response", e)
            sendErrorResponse(ctx, 500, "Error sending response: ${e.message}")
        }
    }

    fun sendErrorResponse(ctx: RoutingContext, statusCode: Int, message: String) {
        try {
            val response = JsonObject()
                .put("error", message)
            
            ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(response.encode())
        } catch (e: Exception) {
            logger.error("Error sending error response", e)
            // Fallback to simple text response if JSON fails
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "text/plain")
                .end("Internal server error")
        }
    }

    fun flagToParam(flag: String): String {
        return when (flag) {
            "disable-schema" -> "--disable-schema"
            "disable-constraint" -> "--disable-constraint"
            else -> throw IllegalArgumentException("Unknown flag: $flag")
        }
    }

    fun mapMimeTypeToFormat(acceptHeader: String?, formatParam: String?): String {
        // If format parameter is provided, use it
        formatParam?.let {
            return when (it.lowercase()) {
                "json" -> "json"
                "xml" -> "xml"
                "yaml" -> "yaml"
                else -> "json" // default to JSON for unknown formats
            }
        }

        // Otherwise try to map from Accept header
        return when {
            acceptHeader == null -> "json"
            acceptHeader.contains("application/json") -> "json"
            acceptHeader.contains("text/xml") || acceptHeader.contains("application/xml") -> "xml"
            acceptHeader.contains("text/yaml") || acceptHeader.contains("application/yaml") -> "yaml"
            else -> "json" // default to JSON
        }
    }

    fun mapFormatToMimeType(format: String): String {
        return when (format.lowercase()) {
            "json" -> "application/json"
            "xml" -> "text/xml"
            "yaml" -> "text/yaml"
            else -> "application/json" // default to JSON
        }
    }
}

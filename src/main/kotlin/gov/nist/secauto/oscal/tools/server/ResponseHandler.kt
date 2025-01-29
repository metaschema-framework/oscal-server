/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import gov.nist.secauto.metaschema.cli.processor.ExitStatus
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import java.io.File

class ResponseHandler {
    fun sendSuccessResponse(ctx: RoutingContext, exitStatus: ExitStatus, sarifFilePath: String) {
        val fileContent = File(sarifFilePath).readText()
        ctx.response()
            .setStatusCode(200) // HTTP 200 OK
            .putHeader("Exit-Status", exitStatus.exitCode.toString())
            .end(fileContent)
    }

    fun sendErrorResponse(ctx: RoutingContext, exitCode: Int, message: String) {
        ctx.response()
            .setStatusCode(exitCode)
            .putHeader("Exit-Status", "PROCESSING_ERROR")
            .end(JsonObject().put("error", message).encode())
    }

    fun mapMimeTypeToFormat(mimeType: String?, formatParam: String?): String {
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
                else -> "ERROR"
            }
        }

        // Default to ERROR if neither a valid formatParam nor MIME type is provided
        return "ERROR"
    }

    fun mapFormatToMimeType(format: String?): String {
        return when (format) {
            "JSON" -> "application/json"
            "XML" -> "text/xml"
            "YAML" -> "text/yaml"
            else -> "ERROR"
        }
    }

    fun flagToParam(format: String): String {
        return when (format) {
            "disable-schema" -> "--disable-schema-validation"
            "disable-constraint" -> "--disable-constraint-validation"
            else -> "--quiet"
        }
    }

    fun unescapeXmlString(xml: String): String {
        return xml.replace("\\\"", "\"")  // Replace escaped quotes with regular quotes
                 .replace("\\'", "'")      // Replace escaped single quotes
                 .replace("\\n", "\n")     // Replace escaped newlines
                 .replace("\\r", "\r")     // Replace escaped carriage returns
                 .replace("\\t", "\t")     // Replace escaped tabs
    }
}

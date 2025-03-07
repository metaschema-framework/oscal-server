/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.tools.server

import io.vertx.ext.web.RoutingContext
import io.vertx.core.json.JsonObject
import io.vertx.core.json.JsonArray
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime

class ModuleHandler(private val moduleDir: Path) {
    private val logger: Logger = LogManager.getLogger(ModuleHandler::class.java)

    fun handleListFiles(ctx: RoutingContext) {
        try {
            if (!Files.exists(moduleDir)) {
                Files.createDirectories(moduleDir)
            }

            val files = Files.list(moduleDir).use { paths ->
                paths.map { path ->
                    JsonObject()
                        .put("name", path.fileName.toString())
                        .put("size", path.fileSize())
                        .put("lastModified", path.getLastModifiedTime().toString())
                        .put("mimeType", Files.probeContentType(path) ?: "application/octet-stream")
                }.toList()
            }

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(JsonArray(files).encode())
        } catch (e: Exception) {
            logger.error("Error listing files", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(JsonObject().put("error", "Internal server error").encode())
        }
    }

    fun handleUploadFile(ctx: RoutingContext) {
        try {
            if (!Files.exists(moduleDir)) {
                Files.createDirectories(moduleDir)
            }

            logger.info("Handling content upload")
            val body = ctx.body().asString()
            if (body.isNullOrEmpty()) {
                logger.error("No content provided")
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(JsonObject().put("error", "No content provided").encode())
                return
            }

            val contentType = ctx.request().getHeader("Content-Type")
            val filename = ctx.pathParam("filename")!!
            val targetPath = moduleDir.resolve(filename)
            
            // Handle content based on content type
            when (contentType?.lowercase()) {
                "application/json" -> {
                    // Parse and preserve root level object structure for JSON
                    val jsonObject = JsonObject(body)
                    val rootKey = jsonObject.fieldNames().first()
                    val wrappedContent = JsonObject().put(rootKey, jsonObject.getJsonObject(rootKey))
                    Files.write(targetPath, wrappedContent.encode().toByteArray())
                }
                "text/yaml", "application/yaml", "application/x-yaml" -> {
                    // Store YAML content as-is
                    Files.write(targetPath, body.toByteArray())
                }
                "text/xml", "application/xml" -> {
                    // Store XML content as-is
                    Files.write(targetPath, body.toByteArray())
                }
                else -> {
                    // Default to storing content as-is
                    Files.write(targetPath, body.toByteArray())
                }
            }

            val response = JsonObject()
                .put("name", targetPath.fileName.toString())
                .put("size", Files.size(targetPath))
                .put("lastModified", Files.getLastModifiedTime(targetPath).toString())
                .put("mimeType", contentType ?: "application/octet-stream")

            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(response.encode())
        } catch (e: Exception) {
            logger.error("Error uploading file", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(JsonObject().put("error", "Internal server error").encode())
        }
    }

    fun handleGetFile(ctx: RoutingContext) {
        try {
            val filename = ctx.pathParam("filename")
            val filePath = moduleDir.resolve(filename)

            if (!Files.exists(filePath)) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end(JsonObject().put("error", "File not found").encode())
                return
            }

            val mimeType = Files.probeContentType(filePath) ?: "application/octet-stream"
            ctx.response()
                .putHeader("Content-Type", mimeType)
                .putHeader("Content-Disposition", "attachment; filename=\"${filePath.fileName}\"")
                .sendFile(filePath.toString())
        } catch (e: Exception) {
            logger.error("Error getting file", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(JsonObject().put("error", "Internal server error").encode())
        }
    }

    fun handleUpdateFile(ctx: RoutingContext) {
        try {
            val filename = ctx.pathParam("filename")
            val filePath = moduleDir.resolve(filename)

            logger.info("Handling content update for file $filename")
            val body = ctx.body().asString()
            if (body.isNullOrEmpty()) {
                logger.error("No content provided")
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(JsonObject().put("error", "No content provided").encode())
                return
            }

            val contentType = ctx.request().getHeader("Content-Type")
            // Handle content based on content type
            when (contentType?.lowercase()) {
                "application/json" -> {
                    // Parse and preserve root level object structure for JSON
                    val jsonObject = JsonObject(body)
                    val rootKey = jsonObject.fieldNames().first()
                    val wrappedContent = JsonObject().put(rootKey, jsonObject.getJsonObject(rootKey))
                    Files.write(filePath, wrappedContent.encode().toByteArray())
                }
                "text/yaml", "application/yaml", "application/x-yaml" -> {
                    // Store YAML content as-is
                    Files.write(filePath, body.toByteArray())
                }
                "text/xml", "application/xml" -> {
                    // Store XML content as-is
                    Files.write(filePath, body.toByteArray())
                }
                else -> {
                    // Default to storing content as-is
                    Files.write(filePath, body.toByteArray())
                }
            }
            val response = JsonObject()
                .put("name", filePath.fileName.toString())
                .put("size", Files.size(filePath))
                .put("lastModified", Files.getLastModifiedTime(filePath).toString())
                .put("mimeType", contentType ?: "application/octet-stream")

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(response.encode())
        } catch (e: Exception) {
            logger.error("Error updating file", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(JsonObject().put("error", "Internal server error").encode())
        }
    }

    fun handleDeleteFile(ctx: RoutingContext) {
        try {
            val filename = ctx.pathParam("filename")
            val filePath = moduleDir.resolve(filename)

            if (!Files.exists(filePath)) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end(JsonObject().put("error", "File not found").encode())
                return
            }

            Files.delete(filePath)

            ctx.response()
                .setStatusCode(204)
                .end()
        } catch (e: Exception) {
            logger.error("Error deleting file", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(JsonObject().put("error", "Internal server error").encode())
        }
    }
}

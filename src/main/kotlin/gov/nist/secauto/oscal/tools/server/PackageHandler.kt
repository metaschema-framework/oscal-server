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
class PackageHandler(private val packagesDir: Path) {
    private val logger: Logger = LogManager.getLogger(PackageHandler::class.java)

    private fun ensurePackageDirectory(packageId: String): Path {
        val packageDir = packagesDir.resolve(packageId)
        if (!Files.exists(packageDir)) {
            Files.createDirectories(packageDir)
        }
        return packageDir
    }

    fun handleListPackageFiles(ctx: RoutingContext) {
        try {
            val packageId = ctx.pathParam("packageId")
            val packageDir = ensurePackageDirectory(packageId)

            val files = Files.list(packageDir).use { paths ->
                paths.map { path ->
                    JsonObject()
                        .put("name", path.fileName.toString())
                        .put("size", path.fileSize())
                        .put("lastModified", path.getLastModifiedTime().toString())
                        .put("mimeType", Files.probeContentType(path) ?: "application/octet-stream")
                        .put("path", packageDir.relativize(path.parent ?: packageDir).toString())
                }.toList()
            }

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(JsonArray(files).encode())
        } catch (e: Exception) {
            logger.error("Error listing package files", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(JsonObject().put("error", "Internal server error").encode())
        }
    }

    fun handleUploadPackageFile(ctx: RoutingContext) {
        try {
            val packageId = ctx.pathParam("packageId")
            val packageDir = ensurePackageDirectory(packageId)

            logger.info("Handling content upload for package $packageId")
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
            val targetPath = packageDir.resolve(filename)
            
            // Handle content based on content type
            when (contentType?.lowercase()) {
                "application/json" -> {
                    val jsonObject = JsonObject(body)
                    val rootKey = jsonObject.fieldNames().first()
                    val wrappedContent = JsonObject().put(rootKey, jsonObject.getJsonObject(rootKey))
                    Files.write(targetPath, wrappedContent.encode().toByteArray())
                }
                "text/yaml", "application/yaml", "application/x-yaml",
                "text/xml", "application/xml" -> Files.write(targetPath, body.toByteArray())
                else -> Files.write(targetPath, body.toByteArray())
            }

            val response = JsonObject()
                .put("name", targetPath.fileName.toString())
                .put("size", Files.size(targetPath))
                .put("lastModified", Files.getLastModifiedTime(targetPath).toString())
                .put("mimeType", contentType ?: "application/octet-stream")
                .put("path", packageDir.relativize(targetPath.parent ?: packageDir).toString())

            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(response.encode())
        } catch (e: Exception) {
            logger.error("Error uploading package file", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(JsonObject().put("error", "Internal server error").encode())
        }
    }

    fun handleGetPackageFile(ctx: RoutingContext) {
        try {
            val packageId = ctx.pathParam("packageId")
            val filename = ctx.pathParam("filename")
            val packageDir = ensurePackageDirectory(packageId)
            val filePath = packageDir.resolve(filename)

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
            logger.error("Error getting package file", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(JsonObject().put("error", "Internal server error").encode())
        }
    }

    fun handleUpdatePackageFile(ctx: RoutingContext) {
        try {
            val packageId = ctx.pathParam("packageId")
            val filename = ctx.pathParam("filename")
            val packageDir = ensurePackageDirectory(packageId)
            val filePath = packageDir.resolve(filename)

            logger.info("Handling content update for package $packageId, file $filename")
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
            when (contentType?.lowercase()) {
                "application/json" -> {
                    val jsonObject = JsonObject(body)
                    val rootKey = jsonObject.fieldNames().first()
                    val wrappedContent = JsonObject().put(rootKey, jsonObject.getJsonObject(rootKey))
                    Files.write(filePath, wrappedContent.encode().toByteArray())
                }
                "text/yaml", "application/yaml", "application/x-yaml",
                "text/xml", "application/xml" -> Files.write(filePath, body.toByteArray())
                else -> Files.write(filePath, body.toByteArray())
            }

            val response = JsonObject()
                .put("name", filePath.fileName.toString())
                .put("size", Files.size(filePath))
                .put("lastModified", Files.getLastModifiedTime(filePath).toString())
                .put("mimeType", contentType ?: "application/octet-stream")
                .put("path", packageDir.relativize(filePath.parent ?: packageDir).toString())

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(response.encode())
        } catch (e: Exception) {
            logger.error("Error updating package file", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(JsonObject().put("error", "Internal server error").encode())
        }
    }

    fun handleDeletePackageFile(ctx: RoutingContext) {
        try {
            val packageId = ctx.pathParam("packageId")
            val filename = ctx.pathParam("filename")
            val packageDir = ensurePackageDirectory(packageId)
            val filePath = packageDir.resolve(filename)

            if (!Files.exists(filePath)) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end(JsonObject().put("error", "File not found").encode())
                return
            }

            Files.delete(filePath)

            // If package directory is empty, delete it
            if (Files.list(packageDir).use { it.count() } == 0L) {
                Files.delete(packageDir)
            }

            ctx.response()
                .setStatusCode(204)
                .end()
        } catch (e: Exception) {
            logger.error("Error deleting package file", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(JsonObject().put("error", "Internal server error").encode())
        }
    }
}

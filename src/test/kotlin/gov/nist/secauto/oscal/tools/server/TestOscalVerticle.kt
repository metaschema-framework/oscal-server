package gov.nist.secauto.oscal.tools.server
import java.nio.file.Files
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Path
import java.nio.file.Paths
import java.io.File
import java.net.URL
import java.net.URLEncoder
import java.net.URI
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager

@ExtendWith(VertxExtension::class)
class TestOscalVerticle {
    private val logger: Logger = LogManager.getLogger(TestOscalVerticle::class.java)
    private lateinit var vertx: Vertx
    private lateinit var webClient: WebClient

    @BeforeEach
    fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        this.vertx = vertx
        webClient = WebClient.create(vertx, WebClientOptions().setDefaultPort(8888))
        initializeOscalDirectory()

        vertx.deployVerticle(OscalVerticle())
            .onComplete(testContext.succeedingThenComplete())
    }

    @Test
    fun test_performance_validation(testContext: VertxTestContext) {
        try {
            // Download base test file
            val url = URL("https://raw.githubusercontent.com/usnistgov/oscal-content/refs/heads/main/examples/ssp/xml/ssp-example.xml")
            val baseFile = downloadToTempFile(url, "perf-base", ".xml")
            
            // Create 20 slightly modified copies
            val testFiles = (1..20).map { index ->
                val content = Files.readString(baseFile)
                val modified = content.replace(
                    "<title>Enterprise Logging and Auditing System Security Plan</title>",
                    "<title>Enterprise Logging and Auditing System Security Plan $index</title>"
                )
                val tempFile = Files.createTempFile(baseFile.parent, "perf-test-$index", ".xml")
                Files.writeString(tempFile, modified)
                tempFile
            }

            var totalTime = 0L
            var successCount = 0
            var currentIndex = 0

            fun validateNext() {
                if (currentIndex >= testFiles.size) {
                    // All files processed - report results
                    val avgTime = if (successCount > 0) totalTime / successCount else 0
                    logger.info("Performance Test Results:")
                    logger.info("Total files: ${testFiles.size}")
                    logger.info("Successful validations: $successCount")
                    logger.info("Average time per validation: ${avgTime}ms")
                    logger.info("Total time: ${totalTime}ms")
                    
                    testContext.verify {
                        assert(successCount > 0) { "At least one validation should succeed" }
                    }
                    testContext.completeNow()
                    return
                }

                val startTime = System.currentTimeMillis()
                val fileUri = testFiles[currentIndex].toUri().toString()

                webClient.get("/validate")
                    .addQueryParam("document", fileUri)
                    .send { ar ->
                        if (ar.succeeded()) {
                            val response = ar.result()
                            if (response.statusCode() == 200) {
                                val endTime = System.currentTimeMillis()
                                totalTime += (endTime - startTime)
                                successCount++
                                logger.info("Validated file ${currentIndex + 1}/20 in ${endTime - startTime}ms")
                            }
                        }
                        currentIndex++
                        validateNext()
                    }
            }

            // Start the validation chain
            validateNext()

        } catch (e: Exception) {
            logger.error("Performance test failed", e)
            testContext.failNow(e)
        }
    }

    @AfterEach
    fun tearDown(testContext: VertxTestContext) {
        vertx.close().onComplete(testContext.succeedingThenComplete())
    }

    @Test
    fun test_oscal_command_remote(testContext: VertxTestContext) {
        try {
            // Download the file first
            val url = URL("https://raw.githubusercontent.com/usnistgov/oscal-content/refs/heads/main/examples/ssp/xml/ssp-example.xml")
            val tempFile = downloadToTempFile(url, "remote-test", ".xml")
            val fileUri = tempFile.toUri().toString()
            
            logger.info("Testing remote validation with local file: $fileUri")
            
            webClient.get("/validate")
                .addQueryParam("document", fileUri)
                .send(testContext.succeeding { response -> 
                    testContext.verify { 
                        assertEquals(200, response.statusCode())
                        val body = response.bodyAsJsonObject()
                        assertNotNull(body)
                        assertTrue(body.containsKey("runs"))
                        testContext.completeNow()
                    }
                })
        } catch (e: Exception) {
            logger.error("Error in remote validation test", e)
            testContext.failNow(e)
        }
    }
    private fun initializeOscalDirectory() {
        val homeDir = System.getProperty("user.home")
        val oscalDir = Paths.get(homeDir, ".oscal")
        val oscalTmpDir = oscalDir.resolve("tmp")
        if (!Files.exists(oscalDir)) {
            Files.createDirectory(oscalDir)
        }
        if (!Files.exists(oscalTmpDir)) {
            Files.createDirectory(oscalTmpDir)
        }
    }
    
    @Test
    fun test_oscal_command_local_file(testContext: VertxTestContext) {
        try {
            // Download the file
            val url = URL("https://raw.githubusercontent.com/usnistgov/oscal-content/refs/heads/main/examples/ssp/xml/ssp-example.xml")
            val homeDir = System.getProperty("user.home")
            val oscalDir = Paths.get(homeDir, ".oscal")

            // Ensure the OSCAL directory exists
            if (!Files.exists(oscalDir)) {
                Files.createDirectories(oscalDir)
                logger.info("Created OSCAL directory: $oscalDir")
            }

            var tempFile: Path? = null
            try {
                tempFile = Files.createTempFile(oscalDir, "ssp", ".xml")
                logger.info("Created temporary file: $tempFile")
            } catch (e: Exception) {
                logger.error("Failed to create temporary file in $oscalDir", e)
                testContext.failNow(e)
                return
            }

            val tempFilePath = tempFile.toAbsolutePath()

            runBlocking<Unit> {
                try {
                    url.openStream().use { input ->
                        Files.newOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    logger.info("Successfully downloaded content to $tempFile")
                } catch (e: Exception) {
                    logger.error("Failed to download or write content", e)
                    testContext.failNow(e)
                    return@runBlocking
                }
            }

            val fileUrl = "file://" + URLEncoder.encode(tempFilePath.toString(), "UTF-8")

            webClient.get("/validate")
                .addQueryParam("document", fileUrl)
                .send(testContext.succeeding { response ->
                    testContext.verify {
                        assertEquals(200, response.statusCode())
                        val body = response.bodyAsJsonObject()
                        assertEquals("OK", response.getHeader("Exit-Status"))
                        assertNotNull(body)
                        assertTrue(body.containsKey("runs"))
                        testContext.completeNow()
                    }
                })
        } catch (e: Exception) {
            logger.error("Unexpected error in test", e)
            testContext.failNow(e)
        }
    }

    @Test
    fun test_oscal_command_resolve_high_baseline(testContext: VertxTestContext) {
        val url = URL("https://raw.githubusercontent.com/GSA/fedramp-automation/refs/heads/develop/src/content/rev5/baselines/xml/FedRAMP_rev5_HIGH-baseline_profile.xml")

        val file = downloadFile(url)
        downloadFile(URL("https://raw.githubusercontent.com/GSA/fedramp-automation/refs/heads/develop/src/content/rev5/baselines/xml/FedRAMP_rev5_catalog_tailoring_profile.xml"));

        try {
            // Ensure the catalog file has the expected name that the resolver will look for
            // The profile typically references the catalog by a specific name

            val fileUri = file.toUri().toString()
            logger.info("Resolving HIGH baseline profile at: $fileUri")

            webClient.get("/resolve")
                .addQueryParam("document", fileUri)
                .putHeader("Accept", "application/json")
                .send(testContext.succeeding { response ->
                    testContext.verify {
                        assertEquals(200, response.statusCode())
                        val body = response.bodyAsJsonObject()
                        assertEquals("OK", response.getHeader("Exit-Status"))
                        assertNotNull(body)
                        
                        // Verify that the resolved profile contains HIGH baseline specific content
                        val bodyString = body.toString()
                        assertTrue(bodyString.contains("HIGH-baseline"), "Resolved profile should contain HIGH-baseline content")
                        
                        testContext.completeNow()
                    }
                })
        } finally {
            // Files.deleteIfExists(file)
        }
    }

    @Test
    fun test_oscal_command_convert(testContext: VertxTestContext) {
        val url = URL("https://raw.githubusercontent.com/usnistgov/oscal-content/main/examples/catalog/xml/basic-catalog.xml")
        val tempFile = downloadToTempFile(url, "convert", ".xml")

        try {
            val fileUri = tempFile.toUri().toString()

            webClient.get("/convert")
                .addQueryParam("document", fileUri)
                .putHeader("Accept", "application/json")
                .send(testContext.succeeding { response ->
                    testContext.verify {
                        assertEquals(200, response.statusCode())
                        val body = response.bodyAsJsonObject()
                        assertEquals("OK", response.getHeader("Exit-Status"))
                        assertNotNull(body)
                        testContext.completeNow()
                    }
                })
        } finally {
            // Files.deleteIfExists(tempFile)
        }
    }

    private fun downloadToTempFile(url: URL, prefix: String, suffix: String): Path {
        val homeDir = System.getProperty("user.home")
        val oscalDir = Paths.get(homeDir, ".oscal")
        val tempFile = Files.createTempFile(oscalDir, prefix, suffix)

        runBlocking {
            try {
                url.openStream().use { input ->
                    Files.newOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                logger.info("Successfully downloaded content to $tempFile")
            } catch (e: Exception) {
                logger.error("Failed to download or write content", e)
                throw e
            }
        }

        return tempFile
    }
    
    private fun downloadFile(url: URL): Path {
        val homeDir = System.getProperty("user.home")
        val oscalDir = Paths.get(homeDir, ".oscal")
        
        // Extract the original filename from the URL
        val fileName = Paths.get(url.path).fileName.toString()
        val file = oscalDir.resolve(fileName)

        runBlocking {
            try {
                url.openStream().use { input ->
                    Files.newOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                logger.info("Successfully downloaded content to $file with original filename")
            } catch (e: Exception) {
                logger.error("Failed to download or write content", e)
                throw e
            }
        }

        return file
    }
    private fun downloadCatalog(catalogUrl: URL, targetDir: Path): Path {
        val catalogFileName = Paths.get(catalogUrl.path).fileName
        val catalogFile = targetDir.resolve(catalogFileName)

        runBlocking {
            try {
                catalogUrl.openStream().use { input ->
                    Files.newOutputStream(catalogFile).use { output ->
                        input.copyTo(output)
                    }
                }
                logger.info("Successfully downloaded catalog to $catalogFile")
            } catch (e: Exception) {
                logger.error("Failed to download or write catalog", e)
                throw e
            }
        }

        return catalogFile
    }
    @Test
    fun test_validate_with_constraint_increases_rule_count(testContext: VertxTestContext) {
        try {
            // Download and save test files
            val sspUrl = URL("https://raw.githubusercontent.com/wandmagic/fedramp-automation/refs/heads/feature/external-constraints/src/validations/constraints/content/ssp-attachment-type-INVALID.xml")
            val constraintUrl = URL("https://raw.githubusercontent.com/GSA/fedramp-automation/refs/heads/develop/src/validations/constraints/fedramp-external-constraints.xml")
            
            val sspFile = downloadToTempFile(sspUrl, "ssp", ".xml")
            val constraintFile = downloadToTempFile(constraintUrl, "constraints", ".xml")

            val sspFileUri = sspFile.toUri().toString()
            val constraintFileUri = constraintFile.toUri().toString()

            // Validate without constraint
            webClient.get("/validate")
                .addQueryParam("flag", "disable-schema")
                .addQueryParam("document", sspFileUri)
                .send { arWithoutConstraint ->
                    if (arWithoutConstraint.succeeded()) {
                        val responseWithoutConstraint = arWithoutConstraint.result()
                        val sarifWithoutConstraint = responseWithoutConstraint.bodyAsString()
                        val ruleCountWithoutConstraint = (sarifWithoutConstraint).length

                        // Validate with constraint
                        webClient.get("/validate")
                            .addQueryParam("document", sspFileUri)
                            .addQueryParam("flag", "disable-schema")
                            .addQueryParam("constraint", constraintFileUri)
                            .send { arWithConstraint ->
                                if (arWithConstraint.succeeded()) {
                                    val responseWithConstraint = arWithConstraint.result()
                                    val sarifWithConstraint = responseWithConstraint.bodyAsString()
                                    val ruleCountWithConstraint = (sarifWithConstraint).length

                                    // Verify that the number of rules has increased
                                    testContext.verify {
                                        assertEquals(200, responseWithoutConstraint.statusCode())
                                        assertEquals(200, responseWithConstraint.statusCode())
                                        assertTrue(ruleCountWithConstraint > ruleCountWithoutConstraint,
                                            "Rule count with constraint ($ruleCountWithConstraint) should be greater than without constraint ($ruleCountWithoutConstraint)")
                                        assertTrue(sarifWithConstraint.contains("resource-has-title"))
                                        assertFalse(sarifWithoutConstraint.contains("resource-has-title"))
                                        testContext.completeNow()
                                    }
                                } else {
                                    logger.error("Validation with constraint request failed", arWithConstraint.cause())
                                    testContext.failNow(arWithConstraint.cause())
                                }
                            }
                    } else {
                        logger.error("Validation without constraint request failed", arWithoutConstraint.cause())
                        testContext.failNow(arWithoutConstraint.cause())
                    }
                }
        } catch (e: Exception) {
            logger.error("Unexpected error in test", e)
            testContext.failNow(e)
        }
    }


}

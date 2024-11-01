package gov.nist.secauto.oscal.tools.server
import java.nio.file.Files
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Path
import java.nio.file.Paths
import java.io.File
import java.net.URL
import java.net.URLEncoder
import java.net.URI
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager

@RunWith(VertxUnitRunner::class)
class TestOscalVerticle {
    private val logger: Logger = LogManager.getLogger(TestOscalVerticle::class.java)
    private lateinit var vertx: Vertx
    private lateinit var webClient: WebClient

    @Before
    fun setUp(testContext: TestContext) {
        vertx = Vertx.vertx()
        webClient = WebClient.create(vertx, WebClientOptions().setDefaultPort(8888))
        initializeOscalDirectory()

        val async = testContext.async()
        vertx.deployVerticle(OscalVerticle()) { ar ->
            if (ar.succeeded()) {
                async.complete()
            } else {
                testContext.fail(ar.cause())
            }
        }
    }

    @After
    fun tearDown(testContext: TestContext) {
        vertx.close(testContext.asyncAssertSuccess())
    }

    @Test
    fun test_oscal_command_remote(testContext: TestContext) {
        val async = testContext.async()

        webClient.get("/validate")
            .addQueryParam("document", "https://raw.githubusercontent.com/usnistgov/oscal-content/refs/heads/main/examples/ssp/xml/ssp-example.xml")
            .send { ar ->
                if (ar.succeeded()) {
                    val response = ar.result()
                    testContext.assertEquals(200, response.statusCode())
                    val body = response.bodyAsJsonObject()
                    testContext.assertNotNull(body)
                    testContext.assertTrue(body.containsKey("runs"))
                    async.complete()
                } else {
                    testContext.fail(ar.cause())
                }
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
    fun test_oscal_command_local_file(testContext: TestContext) {
        val async = testContext.async()

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
                testContext.fail("Failed to create temporary file: ${e.message}")
                return@test_oscal_command_local_file
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
                    testContext.fail("Failed to download or write content: ${e.message}")
                    return@runBlocking
                }
            }

            val fileUrl = "file://" + URLEncoder.encode(tempFilePath.toString(), "UTF-8")

            webClient.get("/validate")
                .addQueryParam("document", fileUrl)
                .send { ar ->
                    if (ar.succeeded()) {
                        val response = ar.result()
                        testContext.assertEquals(200, response.statusCode())
                        val body = response.bodyAsJsonObject()
                        testContext.assertEquals("OK", response.getHeader("Exit-Status"))
                        testContext.assertNotNull(body)
                        testContext.assertTrue(body.containsKey("runs"))
                        async.complete()
                    } else {
                        logger.error("Validation request failed", ar.cause())
                        testContext.fail(ar.cause())
                    }

                }
        } catch (e: Exception) {
            logger.error("Unexpected error in test", e)
            testContext.fail(e)
        }
    }

    @Test
    fun test_oscal_command_resolve(testContext: TestContext) {
        val async = testContext.async()

        val url = URL("https://raw.githubusercontent.com/GSA/fedramp-automation/refs/heads/master/src/content/rev5/baselines/xml/FedRAMP_rev5_LI-SaaS-baseline_profile.xml")
        val catalogUrl = URL("https://raw.githubusercontent.com/GSA/fedramp-automation/refs/heads/master/src/content/rev5/baselines/xml/NIST_SP-800-53_rev5_catalog.xml")

        val tempFile = downloadToTempFile(url, "resolve", ".xml")
        val catalogFile = downloadCatalog(catalogUrl, tempFile.parent)

        try {
            val fileUri = tempFile.toUri().toString()

            webClient.get("/resolve")
                .addQueryParam("document", fileUri)
                .putHeader("Accept", "application/json")
                .send { ar ->
                    if (ar.succeeded()) {
                        val response = ar.result()
                        testContext.assertEquals(200, response.statusCode())
                        val body = response.bodyAsJsonObject()
                        testContext.assertEquals("OK", response.getHeader("Exit-Status"))
                        testContext.assertNotNull(body)
                        async.complete()
                    } else {
                        logger.error("Resolve request failed", ar.cause())
                        testContext.fail(ar.cause())
                    }
                }
        } finally {
            // Files.deleteIfExists(tempFile)
            // Files.deleteIfExists(catalogFile)
        }
    }
    @Test
    fun test_oscal_command_validate_metaschema_content(testContext: TestContext) {
        val async = testContext.async()

        val url = ("https://raw.githubusercontent.com/wandmagic/fedramp-automation/refs/heads/feature/external-constraints/src/validations/constraints/unit-tests/attachment-type-FAIL.yaml")
        val moduleUrl = ("https://raw.githubusercontent.com/aj-stein-gsa/fedramp-automation/bd294a32d23114bdcf52aeeca2d81f785fd5bc37/src/validations/constraints/unit-tests/unit_test_metaschema.xml")

        try {
            webClient.get("/validate")
                .addQueryParam("document", url)
                .addQueryParam("module", moduleUrl)
                .send { ar ->
                    if (ar.succeeded()) {
                        val response = ar.result()
                        testContext.assertEquals(200, response.statusCode())
                        val body = response.bodyAsJsonObject()
                        testContext.assertEquals("OK", response.getHeader("Exit-Status"))
                        testContext.assertNotNull(body)
                        async.complete()
                    } else {
                        logger.error("Resolve request failed", ar.cause())
                        testContext.fail(ar.cause())
                    }
                }
        } finally {
        }
    }

    @Test
    fun test_oscal_command_convert(testContext: TestContext) {
        val async = testContext.async()

        val url = URL("https://raw.githubusercontent.com/usnistgov/oscal-content/main/examples/catalog/xml/basic-catalog.xml")
        val tempFile = downloadToTempFile(url, "convert", ".xml")

        try {
            val fileUri = tempFile.toUri().toString()

            webClient.get("/convert")
                .addQueryParam("document", fileUri)
                .putHeader("Accept", "application/json")
                .send { ar ->
                    if (ar.succeeded()) {
                        val response = ar.result()
                        testContext.assertEquals(200, response.statusCode())
                        val body = response.bodyAsJsonObject()
                        testContext.assertEquals("OK", response.getHeader("Exit-Status"))
                        testContext.assertNotNull(body)
                        async.complete()
                    } else {
                        logger.error("Convert request failed", ar.cause())
                        testContext.fail(ar.cause())
                    }
                }
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
    fun test_validate_with_constraint_increases_rule_count(testContext: TestContext) {
        val async = testContext.async()

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
                        testContext.assertEquals(200, responseWithoutConstraint.statusCode())
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
                                    testContext.assertEquals(200, responseWithConstraint.statusCode())
                                    val sarifWithConstraint = responseWithConstraint.bodyAsString()
                                    val ruleCountWithConstraint = (sarifWithConstraint).length

                                    // Verify that the number of rules has increased
                                    testContext.assertTrue(ruleCountWithConstraint > ruleCountWithoutConstraint,
                                        "Rule count with constraint ($ruleCountWithConstraint) should be greater than without constraint ($ruleCountWithoutConstraint)")
                                    testContext.assertTrue(sarifWithConstraint.contains("resource-has-title"))
                                    testContext.assertFalse(sarifWithoutConstraint.contains("resource-has-title"))
                                    async.complete()
                                } else {
                                    logger.error("Validation with constraint request failed", arWithConstraint.cause())
                                    testContext.fail(arWithConstraint.cause())
                                }
                            }
                    } else {
                        logger.error("Validation without constraint request failed", arWithoutConstraint.cause())
                        testContext.fail(arWithoutConstraint.cause())
                    }
                }
        } catch (e: Exception) {
            logger.error("Unexpected error in test", e)
            testContext.fail(e)
        }
    }

}
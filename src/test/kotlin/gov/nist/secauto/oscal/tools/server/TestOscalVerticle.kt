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
            .addQueryParam("content", "https://raw.githubusercontent.com/usnistgov/oscal-content/refs/heads/main/examples/ssp/xml/ssp-example.xml")
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
                .addQueryParam("content", fileUrl)
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

                    // Clean up the temporary file
                    try {
                        Files.deleteIfExists(tempFile)
                        logger.info("Deleted temporary file: $tempFile")
                    } catch (e: Exception) {
                        logger.warn("Failed to delete temporary file: $tempFile", e)
                    }
                }
        } catch (e: Exception) {
            logger.error("Unexpected error in test", e)
            testContext.fail(e)
        }
    }
}
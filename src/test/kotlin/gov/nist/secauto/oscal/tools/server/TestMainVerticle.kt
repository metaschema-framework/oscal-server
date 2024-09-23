package gov.nist.secauto.oscal.tools.server

import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class TestMainVerticle {

    private lateinit var vertx: Vertx
    private lateinit var webClient: WebClient

    @Before
    fun setUp(testContext: TestContext) {
        vertx = Vertx.vertx()
        webClient = WebClient.create(vertx, WebClientOptions().setDefaultPort(8888))
        
        val async = testContext.async()
        vertx.deployVerticle(MainVerticle()) { ar ->
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
    fun test_run_cli_command(testContext: TestContext) {
        val async = testContext.async()

        webClient.get("/run-cli")
            .addQueryParam("command", "metaschema metapath list-functions")
            .send { ar ->
                if (ar.succeeded()) {
                    val response = ar.result()
                    testContext.assertEquals(200, response.statusCode())

                    val body = response.bodyAsJsonObject()
                    testContext.assertNotNull(body)
                    testContext.assertTrue(body.containsKey("exitCode"))
                    testContext.assertTrue(body.containsKey("status"))

                    val exitCode = body.getInteger("exitCode")
                    testContext.assertEquals(0, exitCode, "Expected exit code 0, but got $exitCode")

                    async.complete()
                } else {
                    testContext.fail(ar.cause())
                }
            }
    }
}
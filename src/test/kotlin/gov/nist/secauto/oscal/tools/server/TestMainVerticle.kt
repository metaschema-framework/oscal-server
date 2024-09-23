package gov.nist.secauto.oscal.tools.server

import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class TestMainVerticle {

  @Rule
  @JvmField
  val rule = RunTestOnContext()

  @Before
  fun deploy_verticle(testContext: TestContext) {
    val vertx = rule.vertx()
    vertx.deployVerticle(MainVerticle()).onComplete(testContext.asyncAssertSuccess())
  }

  @Test
  fun verticle_deployed(testContext: TestContext) {
    val async = testContext.async()
    async.complete()
  }
}

package com.atomist.rug.kind.http

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.project.{Executor, SimpleProjectOperationArguments}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.kind.service._
import com.atomist.rug.runtime.js.interop.jsPathExpressionEngine
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.http.HttpMethod
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.`match`.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators

class HttpTypeUsageTest extends FlatSpec with Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  class DummyServiceSource(reviewOutput: Option[ReviewOutputPolicy] = None) extends ServiceSource {
    var latest: Map[Service, ArtifactSource] = Map()

    override def pathExpressionEngine(): jsPathExpressionEngine =
      new jsPathExpressionEngine()

    override def messageBuilder: MessageBuilder =
      new ConsoleMessageBuilder("TEAM_ID", EmptyActionRegistry)

    override def services: Seq[Service] = Seq()
  }

  // TODO now Spring injection is gone, we need to find another way to test this
  it should "be able to put to an endpoint" in pendingUntilFixed {
    //  Ideally the syntax should be instead:
    //
    //  let res = httpPut "http://someplace" "{'foo': 'bar'}"
    //  with res r
    //    with songs s ...
    val executor =
      """
        |executor DoAnHttpPost
        |
        |with http h begin
        |  do putJson "http://blah" "{\"foo\": \"bar\"}"
        |end
      """.stripMargin

    val arch = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(atomistConfig.executorsRoot + "/DoAnHttpPost.rug", executor))
    val rp = new DefaultRugPipeline()
    val exs = rp.create(arch, None, Nil)
    exs.size should be(1)
    val ex: Executor = exs.collect {
      case e: Executor => e
    }.head
    val ts = new DummyServiceSource

    MockRestServer.server
      .expect(once(), MockRestRequestMatchers.requestTo("http://blah"))
      .andExpect(MockRestRequestMatchers.method(HttpMethod.PUT))
      .andRespond(MockRestResponseCreators.withSuccess())

    ex.execute(ts, SimpleProjectOperationArguments.Empty)

    MockRestServer.server.verify()
  }
}

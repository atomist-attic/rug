package com.atomist.tree.marshal

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.rugdoc.TypeScriptStubClassGeneratorTest
import com.atomist.rug.runtime.js.interop.{NashornUtilsTest, jsSafeCommittingProxy}
import com.atomist.rug.spi.TypeRegistry
import com.atomist.util.lang.JavaScriptArray
import org.scalatest.{FlatSpec, Matchers}

class ProxyAgainstDeserializedNodeTest extends FlatSpec with Matchers {

  private val IssueNode =
    """[{"updatedAt":"2017-04-10T21:01:16Z","body":"test","createdAt":"2017-04-10T21:01:16Z",
      |"closedAt":"","id":"220769383","timestamp":"2017-04-10T21:01:16Z","nodeId":8684,
      |"action":"opened","number":307,"title":"teset","type":["Issue"],"state":"open"},
      |{"name":"Christian Dupuis","login":"cdupuis","nodeId":2453,"type":["GitHubId"]},
      |{"owner":"atomisthqa","name":"handlers","nodeId":7444,"type":["Repo"]},
      |{"name":"handlers","id":"C31PNRDNV","nodeId":7451,"type":["ChatChannel","SlackChannel"]},{"forename":"Christian","surname":"Dupuis","nodeId":1564,"type":["Person"]},
      |{"id":"U1L22E3SA","screenName":"cd","nodeId":1562,"type":["ChatId","SlackId"]},{"startNodeId":8684,"endNodeId":2453,"type":"openedBy","cardinality":"1:1"},
      |{"startNodeId":8684,"endNodeId":7444,"type":"repo","cardinality":"1:1"},
      |{"startNodeId":7444,"endNodeId":8684,"type":"issue","cardinality":"1:M"},
      |{"startNodeId":7444,"endNodeId":7451,"type":"channels","cardinality":"1:M"},{"startNodeId":7451,"endNodeId":7444,"type":"repos","cardinality":"1:M"},
      |{"startNodeId":1564,"endNodeId":2453,"type":"gitHubId","cardinality":"1:1"},{"startNodeId":2453,"endNodeId":1564,"type":"person","cardinality":"1:1"},
      |{"startNodeId":1564,"endNodeId":1562,"type":"chatId","cardinality":"1:1"},{"startNodeId":1562,"endNodeId":1564,"type":"person","cardinality":"1:1"}]
      |""".stripMargin

  "proxy against deserialized node" should "handle simple case without type information" in
    handleSimpleCase(DefaultTypeRegistry)

  it should "handle simple case with type information" in
    handleSimpleCase(DefaultTypeRegistry + TypeScriptStubClassGeneratorTest.cortexTypeRegistry)

  private def handleSimpleCase(tr: TypeRegistry): Unit = {
    val gn: GraphNode = LinkedJsonGraphDeserializer.fromJson(IssueNode)
    val proxy = new jsSafeCommittingProxy(gn, tr)
    proxy.getMember("number") match {
      case x => assert(x.toString === "307")
    }
    val eng = NashornUtilsTest.createEngine
    eng.put("issue", proxy)
    assert(eng.eval("issue.number").toString === "307")
    assert(eng.eval("issue.repo.name").toString === "handlers")
    eng.eval("issue.repo.channels") match {
      case jsa: JavaScriptArray[_] =>
        assert(jsa.size === 1)
      case f => fail(s"Unexpected: $f")
    }
    assert(eng.eval("issue.repo.channels[0].name") === "handlers")
    assert(eng.eval("issue.repo.channels[0].id") === "C31PNRDNV")
    assert(eng.eval("issue.repo.channels.length") === 1)
  }

}

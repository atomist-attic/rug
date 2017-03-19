package com.atomist.tree

import com.atomist.graph.GraphNode
import com.atomist.rug.TestUtils
import com.atomist.rug.runtime.js.interop.NashornMapBackedGraphNode
import com.atomist.rug.runtime.js.{JavaScriptContext, SimpleContainerGraphNode}
import com.atomist.rug.test.gherkin.handler.event.EventHandlerTestTargets
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.pathexpression.PathExpressionEngine
import org.scalatest.{FlatSpec, Matchers}

class QueryByExampleTest extends FlatSpec with Matchers {

  val ee = new PathExpressionEngine

  val nodesFile = TestUtils.requiredFileInPackage(EventHandlerTestTargets, "Nodes.ts").withPath(".atomist/editors/Nodes.ts")

  val as = SimpleFileBasedArtifactSource(nodesFile)
  val cas = TypeScriptBuilder.compileWithModel(as)

  // We pass in Javascript to avoid need for compilation
  "queryByExample" should "work with root" in
    verifyPaths(
      """
        |var node = require("./Nodes");
        |var QueryByExample_1 = require("@atomist/rug/tree/QueryByExample");
        |createdObject = new node.Commit();
        |pathExpression = QueryByExample_1.queryByExample(createdObject);
      """.stripMargin)

  it should "work with one level of predicate" in
    verifyPaths(
      """
        |var node = require("./Nodes");
        |var QueryByExample_1 = require("@atomist/rug/tree/QueryByExample");
        |createdObject = new node.Commit().withMadeBy(
        | new node.Person("Ebony")
        |);
        |pathExpression = QueryByExample_1.queryByExample(createdObject);
      """.stripMargin,
      validator = s => s.contains("[@name='Ebony']]")
    )

  it should "work with JSON and one level of predicate" in
    verifyPaths(
      """
        |var QueryByExample_1 = require("@atomist/rug/tree/QueryByExample");
        |createdObject = {
        | nodeTags: [ "Commit", "dynamic-" ],
        | nodeName: "Commit",
        | madeBy: { nodeTags: [ "Person", "dynamic-"], nodeName: "Ebony", name: "Ebony" }
        |};
        |pathExpression = QueryByExample_1.queryByExample(createdObject);
      """.stripMargin,
      validator = s => s.contains("[@name='Ebony']]")
    )

  it should "work with two levels of predicate" in
    verifyPaths(
      """
        |var node = require("./Nodes");
        |var QueryByExample_1 = require("@atomist/rug/tree/QueryByExample");
        |createdObject = new node.Commit().withMadeBy(
        | new node.Person("Ebony")
        |   .withGitHubId(new node.GitHubId("gogirl"))
        |);
        |pathExpression = QueryByExample_1.queryByExample(createdObject);
      """.stripMargin,
      validator = _.contains("[@id='gogirl']"))

  it should "work with two levels of predicate to match leaf" in
    verifyPaths(
      """
        |var node = require("./Nodes");
        |var QueryByExample_1 = require("@atomist/rug/tree/QueryByExample");
        |
        |var ghid = new node.GitHubId("gogirl")
        |ghid._match = true
        |createdObject = new node.Commit().withMadeBy(
        | new node.Person("Ebony")
        |   .withGitHubId(ghid)
        |);
        |pathExpression = QueryByExample_1.queryByExample(createdObject);
      """.stripMargin,
      validator = _.contains("[@id='gogirl']"),
      matchTestO = Some(n => n.nodeTags.contains("GitHubId")))


  import com.atomist.tree.pathexpression.PathExpressionParser._

  // Scripts must create two references: pathExpression and createdObject
  private def verifyPaths(js: String, validator: String => Boolean = pe => true,
                          matchTestO: Option[GraphNode => Boolean] = None): Unit = {
    val as = cas +
      StringFileArtifact(".atomist/editors/QueryByExample1.js",
        js)
    val jsc = new JavaScriptContext(as)



    val actualExpression = jsc.engine.eval("pathExpression.expression")
    val createdObject = jsc.engine.eval("createdObject")
    actualExpression match {
      case expr: String =>
        // Check that the path matches against the rootObject
        val gn: GraphNode = NashornMapBackedGraphNode.toGraphNode(createdObject)
          .getOrElse(throw new IllegalArgumentException(s"Not a valid graph node: $createdObject"))
        val root = SimpleContainerGraphNode("root", gn)
        val matchTest: GraphNode => Boolean = matchTestO.getOrElse(_ == gn)

        //println(s"Evaluating [$expr] against [$gn]")

        ee.evaluate(root, expr) match {
          case Right(nodes) if nodes.size == 1 =>
            assert(validator(expr))
            assert(matchTest(nodes.head))
          case x => fail(s"Unexpected evaluation result: $x")
        }
      case x => fail(s"Unexpected path expression [$x]")
    }
  }

}

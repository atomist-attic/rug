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

// TODO this really tests TypeScript not Scala, except for path expressions
// So we should really test it with a TypeScript test framework in a separate module
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

  it should "work with JSON and one level of predicate" in pendingUntilFixed(
    verifyPaths(
      """
        |var QueryByExample_1 = require("@atomist/rug/tree/QueryByExample");
        |createdObject = {
        | nodeTags: [ "Commit", "dynamic-" ],
        | nodeName: "Commit",
        | madeBy: { nodeTags: [ "Person", "dynamic-"], nodeName: "Person", name: "Ebony" }
        |};
        |pathExpression = QueryByExample_1.queryByExample(createdObject);
      """.stripMargin,
      validator = s => s.contains("[@name='Ebony']]")
    )
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
        |print(createdObject)
        |pathExpression = QueryByExample_1.queryByExample(createdObject);
        |
        |QueryByExample_1.addAddressesToGraph(createdObject);
      """.stripMargin,
      validator = _.contains("[@id='gogirl']"))


  import com.atomist.tree.pathexpression.PathExpressionParser._

  // Scripts must create two references: pathExpression and createdObject
  private def verifyPaths(js: String, validator: String => Boolean = pe => true): Unit = {
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
        println(s"Evaluating [$expr] against [$gn]")

        ee.evaluate(root, expr) match {
          case Right(nodes) if nodes.size == 1 =>
            assert(nodes.head === gn, "Should match the created node, not subnodes")
            assert(validator(expr))
          case x => fail(s"Unexpected evaluation result: $x")
        }
      case x => fail(s"Unexpected path expression [$x]")
    }
  }

}

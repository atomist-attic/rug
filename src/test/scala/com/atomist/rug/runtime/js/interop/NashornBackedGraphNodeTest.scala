package com.atomist.rug.runtime.js.interop

import com.atomist.graph.AddressableGraphNode
import com.atomist.tree.TreeNode
import jdk.nashorn.api.scripting.{NashornScriptEngine, NashornScriptEngineFactory}
import org.scalatest.{FlatSpec, Matchers}

class NashornBackedGraphNodeTest extends FlatSpec with Matchers {

  val engine: NashornScriptEngine =
    new NashornScriptEngineFactory()
      .getScriptEngine("--optimistic-types", "--language=es6", "--no-java")
      .asInstanceOf[NashornScriptEngine]

  import com.atomist.rug.runtime.js.interop.NashornMapBackedGraphNode._

  "toGraphNode" should "fail to convert null without error" in {
    toGraphNode(null) shouldBe empty
  }

  it should "fail to convert non Nashorn return without error" in {
    toGraphNode(new Object()) shouldBe empty
  }

  it should "get name and properties from simple node" in {
    val n = engine.eval(
      """
        |{
        |   var x = { nodeName: 'Gangster', forename: 'Johnny', surname: 'Caspar', nodeTags: ["tag1", "tag2"]};
        |   x
        |}
      """.stripMargin
    )
    val gn = toGraphNode(n).get
    assert(gn.nodeName === "Gangster")
    assert(gn.relatedNodeNames === Set("forename", "surname"))
    assert(gn.relatedNodesNamed("surname").size === 1)
    val forename = gn.relatedNodesNamed("surname").head
    assert(forename.asInstanceOf[TreeNode].value === "Caspar")
    assert(gn.relatedNodes.size === 2)
  }

  it should "get tags from simple node" in {
    val n = engine.eval(
      """
        |{
        |   var x = { nodeName: 'Gangster', forename: 'Johnny', surname: 'Caspar', nodeTags: ["tag1", "tag2"]};
        |   x
        |}
      """.stripMargin
    )
    val gn = toGraphNode(n).get
    assert(gn.nodeTags === Set("tag1", "tag2"))
    assert(!gn.isInstanceOf[AddressableGraphNode])
  }

  it should "implement AddressableGraphNode if address provided" in {
    val n = engine.eval(
      """
        |{
        |   var x = { address: "addr", nodeName: 'Gangster', forename: 'Johnny', surname: 'Caspar', nodeTags: ["tag1", "tag2"]};
        |   x
        |}
      """.stripMargin
    )
    val gn = toGraphNode(n).get
    assert(gn.nodeTags === Set("tag1", "tag2"))
    gn match {
      case atn: AddressableGraphNode => assert(atn.address === "addr")
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "get name and properties from nested node" in {
    val rootName = "Gangster"
    val n = engine.eval(
      s"""
        |{
        |   var x = { nodeName: '$rootName', forename: 'Johnny', surname: 'Caspar',
        |     associate: { nodeName: 'Leo', forename: 'Leo', nodeTags: ["Irish"]},
        |     nodeTags: ["tag1", "tag2"]};
        |   x
        |}
      """.stripMargin
    )
    val caspar = toGraphNode(n).get
    assert(caspar.nodeName === rootName)
    assert(caspar.relatedNodeNames === Set("forename", "surname", "Leo"))
    assert(caspar.relatedNodesNamed("surname").size === 1)
    val forename = caspar.relatedNodesNamed("surname").head
    assert(forename.asInstanceOf[TreeNode].value === "Caspar")
    assert(caspar.relatedNodesNamed("associate").size === 0)
    val assoc = caspar.followEdge("associate").head
    assert(assoc.nodeName === "Leo")
    assert(assoc.nodeTags.contains("Irish"))
    assert(assoc.relatedNodesNamed("forename").head.asInstanceOf[TreeNode].value === "Leo")
  }

  it should "get name and properties, including array of object, from nested node" in {
    val rootName = "Gangster"
    val n = engine.eval(
      s"""
         |{
         |   var x = { nodeName: '$rootName', forename: 'Johnny', surname: 'Caspar',
         |     associates: [
         |      { nodeName: 'Leo', forename: 'Leo', nodeTags: ["Irish"]},
         |      { nodeName: 'Tom', forename: 'Tom', nodeTags: ["Irish"]}
         |     ],
         |     nodeTags: ["tag1", "tag2"]};
         |   x
         |}
      """.stripMargin
    )
    val caspar = toGraphNode(n).get
    assert(caspar.nodeName === rootName)
    assert(caspar.relatedNodeNames === Set("forename", "surname", "Leo", "Tom"))
    assert(caspar.relatedNodesNamed("surname").size === 1)
    val forename = caspar.relatedNodesNamed("surname").head
    assert(forename.asInstanceOf[TreeNode].value === "Caspar")
    assert(caspar.relatedNodesNamed("associates").size === 0)
    caspar.followEdge("associates").size should be (2)
    val assoc = caspar.followEdge("associates").head
    assert(assoc.nodeName === "Leo")
    assert(assoc.nodeTags.contains("Irish"))
    assert(assoc.relatedNodesNamed("forename").head.asInstanceOf[TreeNode].value === "Leo")
  }

  it should "handle cycle expressed via scalar" in {
    val rootName = "Gangster"
    val n = engine.eval(
      s"""
         |{
         |   var x = { nodeId: 211, nodeName: '$rootName', forename: 'Johnny', surname: 'Caspar',
         |     associates: [
         |      { nodeName: 'Leo', forename: 'Leo', nodeTags: ["Irish"], protects: { nodeRef: 211 } },
         |      { nodeName: 'Tom', forename: 'Tom', nodeTags: ["Irish"]}
         |     ],
         |     nodeTags: ["tag1", "tag2"]};
         |   x
         |}
      """.stripMargin
    )
    val caspar = toGraphNode(n).get
    assert(caspar.nodeName === rootName)
    assert(caspar.relatedNodeNames === Set("forename", "surname", "Leo", "Tom"))
    assert(caspar.relatedNodesNamed("surname").size === 1)
    val forename = caspar.relatedNodesNamed("surname").head
    assert(forename.asInstanceOf[TreeNode].value === "Caspar")
    assert(caspar.relatedNodesNamed("associates").size === 0)
    caspar.followEdge("associates").size should be (2)
    val assoc = caspar.followEdge("associates").head
    assert(assoc.nodeName === "Leo")
    assert(assoc.nodeTags.contains("Irish"))
    assert(assoc.relatedNodesNamed("forename").head.asInstanceOf[TreeNode].value === "Leo")
    assert(assoc.followEdge("protects").head == caspar)
  }

  it should "handle cycle expressed via array" in {
    val rootName = "Gangster"
    val n = engine.eval(
      s"""
         |{
         |   var x = { nodeId: 211, nodeName: '$rootName', forename: 'Johnny', surname: 'Caspar',
         |     associates: [
         |      { nodeName: 'Leo', forename: 'Leo', nodeTags: ["Irish"], protects: [{ nodeRef: 211 }] },
         |      { nodeName: 'Tom', forename: 'Tom', nodeTags: ["Irish"]}
         |     ],
         |     nodeTags: ["tag1", "tag2"]};
         |   x
         |}
      """.stripMargin
    )
    val caspar = toGraphNode(n).get
    assert(caspar.nodeName === rootName)
    assert(caspar.relatedNodeNames === Set("forename", "surname", "Leo", "Tom"))
    assert(caspar.relatedNodesNamed("surname").size === 1)
    val forename = caspar.relatedNodesNamed("surname").head
    assert(forename.asInstanceOf[TreeNode].value === "Caspar")
    assert(caspar.relatedNodesNamed("associates").size === 0)
    caspar.followEdge("associates").size should be (2)
    val assoc = caspar.followEdge("associates").head
    assert(assoc.nodeName === "Leo")
    assert(assoc.nodeTags.contains("Irish"))
    assert(assoc.relatedNodesNamed("forename").head.asInstanceOf[TreeNode].value === "Leo")
    assert(assoc.followEdge("protects").head == caspar)
  }

  it should "get name and properties from simple node using functions" in
    getNameAndPropertiesFromSimpleNode(
      """
        |{
        |var Commit = (function () {
        |    function Commit() {
        |    }
        |    Commit.prototype.nodeName = function () { return "Commit"; };
        |    Commit.prototype.nodeTags = function () { return ["Commit", "GithubThing"]; };
        |    Commit.prototype.forename = function () { return "Johnny"; };
        |    Commit.prototype.surname = function () { return "Caspar"; };
        |    //Commit.prototype.messThisUp = function (a) { return "Caspar" + a; };
        |    return Commit;
        |}());
        |new Commit()
        |}
      """.stripMargin)

  it should "get name and properties from simple node using functions backed by properties" in
    getNameAndPropertiesFromSimpleNode(
      """
        |{
        |var Commit = (function () {
        |    function Commit() {
        |    }
        |    Commit.prototype._surname = null
        |    Commit.prototype.nodeName = function () { return "Commit"; };
        |    Commit.prototype.nodeTags = function () { return ["Commit", "GithubThing"]; };
        |    Commit.prototype.forename = function () { return "Johnny"; };
        |    Commit.prototype.surname = function () { return this._surname; };
        |    Commit.prototype.setSurname = function (s) { this._surname = s; };
        |    return Commit;
        |}());
        |var c = new Commit()
        |c.setSurname("Caspar")
        |c
        |}
      """.stripMargin)

  private def getNameAndPropertiesFromSimpleNode(s: String) {
    val n = engine.eval(s)
    val gn = toGraphNode(n).get
    assert(gn.nodeName === "Commit")
    //assert(gn.relatedNodeNames === Set("forename", "surname"))
    assert(gn.relatedNodesNamed("surname").size === 1)
    val surname = gn.relatedNodesNamed("surname").head
    assert(surname.asInstanceOf[TreeNode].value === "Caspar")
    assert(gn.relatedNodes.size >= 2)
    assert(gn.nodeTags === Set("Commit", "GithubThing"))
  }

  it should "get name and properties from simple node using function relationship" in {
    val n = engine.eval(
      """
        |{
        |var Commit = (function () {
        |    function Commit() {
        |    }
        |    Commit.prototype.nodeName = function () { return "Commit"; };
        |    Commit.prototype.nodeTags = function () { return ["Commit", "GithubThing"]; };
        |    Commit.prototype.forename = function () { return "Johnny"; };
        |    Commit.prototype.surname = function () { return "Caspar"; };
        |    Commit.prototype.raisedBy = function () { return { nodeName: 'Tom', name: 'Ebony', nodeTags: ["Person"]} };
        |    return Commit;
        |}());
        |new Commit()
        |}
      """.stripMargin
    )
    val gn = toGraphNode(n).get
    assert(gn.nodeName === "Commit")
    val s = gn.followEdge("raisedBy")
    assert(s.size === 1)
    assert(s.head.relatedNodeNames.contains("name"))
  }

}

package com.atomist.rug.runtime.js.interop

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.FileMutableView
import com.atomist.rug.runtime.js.SimpleContainerGraphNode
import com.atomist.rug.ts.Cardinality
import com.atomist.source.StringFileArtifact
import com.atomist.tree.{SimpleTerminalTreeNode, TreeNode}
import com.atomist.util.lang.JavaScriptArray
import jdk.nashorn.api.scripting.JSObject
import org.scalatest.{FlatSpec, Matchers}

class jsSafeCommittingProxyTest extends FlatSpec with Matchers {

  "safe committing proxy" should "not allow invocation of non export function" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    val sc = new jsSafeCommittingProxy(fmv, DefaultTypeRegistry)
    intercept[RugRuntimeException] {
      sc.getMember("bla")
    }
  }

  it should "fail for unregistered command function" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    val sc = new jsSafeCommittingProxy(fmv, DefaultTypeRegistry)
    intercept[RugRuntimeException] {
      sc.getMember("delete")
    }
  }

  it should "allow invocation of navigation method" in {
    val c = SimpleContainerGraphNode("root", SimpleTerminalTreeNode("bar", "baz"), TreeNode.Dynamic)
    val sc = new jsSafeCommittingProxy(c, DefaultTypeRegistry)
    sc.getMember("bar")
  }

  it should "recognize cardinality of single value" in {
    val c = SimpleContainerGraphNode("root",
      SimpleTerminalTreeNode("bar", "baz"))
      .withTag(TreeNode.Dynamic)
    val sc = new jsSafeCommittingProxy(c, DefaultTypeRegistry)
    assert(sc.getMember("bar") === "baz")
  }

  it should "recognize cardinality of single value with array marker" in {
    val c = SimpleContainerGraphNode("root",
      SimpleTerminalTreeNode("bar", "baz", Set(Cardinality.One2Many)))
      .withTag(TreeNode.Dynamic)
    val sc = new jsSafeCommittingProxy(c, DefaultTypeRegistry)
    val value = sc.getMember("bar")
    value match {
      case jsa: JavaScriptArray[_] =>
        assert(jsa.size === 1)
      //assert(jsa.lyst === util.Arrays.asList("baz", "baz2"))
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "recognize cardinality of multiple values" in {
    val c = new SimpleContainerGraphNode("root",
      Seq(SimpleTerminalTreeNode("bar", "baz"),
        SimpleTerminalTreeNode("bar", "baz2")),
      Set(TreeNode.Dynamic)
    )
    val sc = new jsSafeCommittingProxy(c, DefaultTypeRegistry)
    val value = sc.getMember("bar")
    value match {
      case jsa: JavaScriptArray[_] =>
        assert(jsa.size === 2)
       //assert(jsa.lyst === util.Arrays.asList("baz", "baz2"))
      case x => fail(s"Unexpected: $x")
    }
  }

}

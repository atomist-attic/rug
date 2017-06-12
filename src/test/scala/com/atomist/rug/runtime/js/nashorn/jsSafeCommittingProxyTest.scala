package com.atomist.rug.runtime.js.nashorn

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.{FileMutableView, ProjectMutableView}
import com.atomist.rug.runtime.js.SimpleContainerGraphNode
import com.atomist.rug.ts.Cardinality
import com.atomist.source.{EmptyArtifactSource, StringFileArtifact}
import com.atomist.tree.{SimpleTerminalTreeNode, TreeNode}
import jdk.nashorn.internal.runtime.ScriptRuntime
import org.scalatest.{FlatSpec, Matchers}

class jsSafeCommittingProxyTest extends FlatSpec with Matchers {

  "safe committing proxy" should "return undefined on invocation of non export function" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = FileMutableView(f, null)
    val sc = new jsSafeCommittingProxy(fmv, DefaultTypeRegistry)
    sc.getMember("bla") should be (ScriptRuntime.UNDEFINED)
  }

  it should "return undefined for unregistered command function" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = FileMutableView(f, null)
    val sc = new jsSafeCommittingProxy(fmv, DefaultTypeRegistry)
    sc.getMember("delete") should be (ScriptRuntime.UNDEFINED)
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
      case jsa: NashornJavaScriptArray[_] =>
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
      case jsa: NashornJavaScriptArray[_] =>
        assert(jsa.size === 2)
       //assert(jsa.lyst === util.Arrays.asList("baz", "baz2"))
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "produce valid JSON toString for simple structure" in {
    val c = new SimpleContainerGraphNode("root",
      Seq(SimpleTerminalTreeNode("bar", "baz"),
        SimpleTerminalTreeNode("bar", "baz2")),
      Set(TreeNode.Dynamic)
    )
    val sc = new jsSafeCommittingProxy(c, DefaultTypeRegistry)
    val s = sc.toString
    assert(s.contains("baz2"))
  }

  it should "produce useful toString for project node" in {
    val p = new ProjectMutableView(EmptyArtifactSource())
    val sc = new jsSafeCommittingProxy(p, DefaultTypeRegistry)
    val s = sc.toString
    assert(s.contains(classOf[ProjectMutableView].getSimpleName))
  }

}

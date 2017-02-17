package com.atomist.rug.runtime.js.interop

import java.util.Collections

import com.atomist.graph.GraphNode
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.core.{FileMutableView, FileType}
import com.atomist.rug.spi.{TreeNodeBehaviour, TreeNodeBehaviourRegistry}
import com.atomist.source.StringFileArtifact
import com.atomist.tree.TreeNode
import jdk.nashorn.api.scripting.AbstractJSObject
import org.scalatest.{FlatSpec, Matchers}

class jsSafeCommittingProxyTest extends FlatSpec with Matchers {

  it should "not allow invocation of non export function" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    val sc = new jsSafeCommittingProxy(fmv)
    intercept[RugRuntimeException] {
      sc.getMember("bla")
    }
  }

  it should "not allow invocation of export function" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    val sc = new jsSafeCommittingProxy(fmv)
     sc.getMember("setContent")
  }

  it should "not allow invocation of registered command function" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    val fc = new FakeTreeNodeBehaviour
    val sc = new jsSafeCommittingProxy(fmv, new FakeTreeNodeBehaviourRegistry(fc))
    val ajs: AbstractJSObject = sc.getMember("execute").asInstanceOf[AbstractJSObject]
    val afc = ajs.call(fmv, null)
    assert(fc.fmv === fmv)
    afc.asInstanceOf[AnotherFakeCommand].really("This is really working")
  }

  it should "fail for unregistered command function" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    val sc = new jsSafeCommittingProxy(fmv, new FakeTreeNodeBehaviourRegistry)
    intercept[RugRuntimeException] {
      sc.getMember("delete")
    }
  }

}

class FakeTreeNodeBehaviourRegistry(fakeCommand: FakeTreeNodeBehaviour = new FakeTreeNodeBehaviour) extends TreeNodeBehaviourRegistry {

  override def findByNodeAndName(treeNode: GraphNode, name: String): Option[TreeNodeBehaviour[GraphNode]] = {
    name match {
      case "execute" => Option(fakeCommand.asInstanceOf[TreeNodeBehaviour[GraphNode]])
      case _ => Option.empty
    }
  }
}

class FakeTreeNodeBehaviour extends TreeNodeBehaviour[FileMutableView] {
  override def nodeTypes = Collections.singleton("file")

  override def name: String = "execute"

  var fmv: FileMutableView = _

  override def invokeOn(treeNode: FileMutableView): AnyRef = {
    fmv = treeNode
    new AnotherFakeCommand

  }
}

class AnotherFakeCommand {

  def really(s: String): Unit = {
    //println(s)
  }
}

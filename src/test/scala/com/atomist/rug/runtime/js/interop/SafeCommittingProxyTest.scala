package com.atomist.rug.runtime.js.interop

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.core.{FileArtifactMutableView, FileType}
import com.atomist.rug.spi.{Command, CommandRegistry}
import com.atomist.source.StringFileArtifact
import com.atomist.tree.TreeNode
import jdk.nashorn.api.scripting.AbstractJSObject
import org.scalatest.{FlatSpec, FunSuite, Matchers}

class SafeCommittingProxyTest extends FlatSpec with Matchers {

  it should "not allow invocation of non export function" in {
    val typed = new FileType()
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)

    val sc = new SafeCommittingProxy(typed, fmv)
    intercept[RugRuntimeException] {
      sc.getMember("bla")
    }
  }

  it should "not allow invocation of export function" in {
    val typed = new FileType()
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    val sc = new SafeCommittingProxy(typed, fmv)
     sc.getMember("setContent")
  }

  it should "not allow invocation of registred command function" in {
    val typed = new FileType()
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    val fc = new FakeCommand
    val sc = new SafeCommittingProxy(typed, fmv, new FakeCommandRegistry(fc))
    val ajs: AbstractJSObject = sc.getMember("execute").asInstanceOf[AbstractJSObject]
    ajs.call(fmv, null)
    fc.fmv should be(fmv)
  }

  it should "fail for unregistered command function" in {
    val typed = new FileType()
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    val sc = new SafeCommittingProxy(typed, fmv, new FakeCommandRegistry)
    intercept[RugRuntimeException] {
      sc.getMember("delete")
    }
  }

}

class FakeCommandRegistry(fakeCommand: FakeCommand = new FakeCommand) extends CommandRegistry {

  override def findByNodeAndName(treeNode: TreeNode, name: String): Option[Command[TreeNode]] = {
    name match {
      case "execute" => Option(fakeCommand.asInstanceOf[Command[TreeNode]])
      case _ => Option.empty
    }
  }
}

class FakeCommand extends Command[FileArtifactMutableView] {
  override def `type`: String = "file"

  override def name: String = "execute"

  var fmv: FileArtifactMutableView = _

  override def invokeOn(treeNode: FileArtifactMutableView): Unit = {
    fmv = treeNode
  }
}



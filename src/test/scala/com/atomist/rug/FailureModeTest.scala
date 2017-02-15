package com.atomist.rug

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit._
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class FailureModeTest extends FlatSpec with Matchers {

  it should "allow default of no modification without error" in {
    val prog =
      """
        |@description "Update Kube spec to redeploy a service"
        |editor Redeploy
        |
        |with File f
        | when { f.name().contains("80-deployment") };
        |do
        |  replace ".*" "foo";
      """.stripMargin
    tryMod(prog) match {
      case n: NoModificationNeeded =>
      
      case _ => ???
    }
  }

  it should "allow kind block to fail whole editor" in {
    val msg = "What in God's holy name are you blathering about?"
    val prog =
      s"""
        |editor Redeploy
        |
        |with File f;
        |do
        |  fail "$msg";
      """.stripMargin
    tryMod(prog) match {
      case f: FailedModificationAttempt if f.failureExplanation == msg =>
      
      case _ => ???
    }
  }

  private  def tryMod(prog: String): ModificationAttempt = {
    val filename = "whatever.txt"
    val as = new SimpleFileBasedArtifactSource("name",
      Seq(
        StringFileArtifact(filename, "some content")
      )
    )

    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)
    val eds = runtime.createFromString(prog)
    assert(eds.size === 1)
    val pe = eds.head.asInstanceOf[ProjectEditor]
    pe.modify(as, SimpleProjectOperationArguments.Empty)
  }
}

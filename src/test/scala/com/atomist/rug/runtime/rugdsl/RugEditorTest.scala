package com.atomist.rug.runtime.rugdsl

import com.atomist.project.edit._
import com.atomist.project.{ProjectOperation, ProjectOperationArguments, SimpleProjectOperationArguments}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object RugEditorTest {

  val ContentPattern = "Anders .*"

  val SimpleEditorWithoutParameters =
    """editor SimpleEditor
      |
      |with project
      |   do addFile "src/from/typescript" "Anders Hjelsberg is God"
    """.stripMargin

}

class RugEditorTest extends FlatSpec with Matchers {

  import RugEditorTest._

  it should "run simple editor twice and see no change the second time" in {
    invokeAndVerifyIdempotentSimple(StringFileArtifact(s".atomist/editors/SimpleEditor.rug", SimpleEditorWithoutParameters))
  }


  val otherEditor: ProjectEditor = new ProjectEditorSupport {
    override protected def modifyInternal(as: ArtifactSource, pmi: ProjectOperationArguments): ModificationAttempt = {
      SuccessfulModification(as + StringFileArtifact("src/from/typescript", pmi.stringParamValue("otherParam")), Set(), "")
    }

    override def impacts: Set[Impact] = Set()
    override def applicability(as: ArtifactSource): Applicability = Applicability.OK
    override def name: String = "other"
    override def description: String = name
  }


  private def invokeAndVerifyIdempotentSimple(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil) = {
    val as = SimpleFileBasedArtifactSource(tsf)
    val ops = new DefaultRugPipeline(DefaultTypeRegistry).create(as, None)
    val red = ops.head.asInstanceOf[RugDrivenProjectEditor]
    red.name should be("SimpleEditor")
    red.setContext(others)

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    val p = SimpleProjectOperationArguments("", Map[String,Object]())
    red.modify(target, p) match {
      case sm: SuccessfulModification =>
        sm.result.totalFileCount should be(2)
        sm.result.findFile("src/from/typescript").get.content.contains("Anders") should be(true)

        red.modify(sm.result, p) match {
          case _: NoModificationNeeded => //yay
          case sm: SuccessfulModification =>
              fail("That should not have reported modification")
        }
    }
    red
  }
}

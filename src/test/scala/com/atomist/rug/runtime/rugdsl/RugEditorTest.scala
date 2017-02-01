package com.atomist.rug.runtime.rugdsl

import com.atomist.project.edit._
import com.atomist.project.{ProjectOperation, ProjectOperationArguments, SimpleProjectOperationArguments}
import com.atomist.rug.{BadRugException, DefaultRugPipeline}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object RugEditorTest {

  val ContentPattern = "Anders .*"

  val SimpleEditorWithoutParameters =
    """editor SimpleEditor
      |
      |with Project
      |   do addFile "src/from/typescript" "Anders Hjelsberg is God"
    """.stripMargin

  val TwoStepEditor =
    """editor TwoStepEditor
      |
      |let dsf = "DoubleSecretFile"
      |
      |with Project p
      |  begin
      |    do addFile dsf "Probation"
      |    with File f when { !p.fileExists("README") }
      |      do p.addExecutableFile "README" "A Pledge Pin!"
      |  end
    """.stripMargin

}


class RugEditorTest extends FlatSpec with Matchers {

  import RugEditorTest._

  it should "run simple editor twice and see no change the second time" in {
    invokeAndVerifyIdempotentSimple(StringFileArtifact(s".atomist/editors/SimpleEditor.rug", SimpleEditorWithoutParameters))
  }

  val otherEditor: ProjectEditor = new ProjectEditorSupport {
    override protected def modifyInternal(as: ArtifactSource, pmi: ProjectOperationArguments): ModificationAttempt = {
      SuccessfulModification(as + StringFileArtifact("src/from/typescript",
        pmi.stringParamValue("otherParam")))
    }

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

    val p = SimpleProjectOperationArguments("", Map[String, Object]())
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

  it should "run TwoStepEditor and return that something changed" in {
    invokeAndVerifySomethingChanged(StringFileArtifact(".atomist/editors/TwoStepEditor.rug", TwoStepEditor))
  }

  private def invokeAndVerifySomethingChanged(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil) = {
    val as = SimpleFileBasedArtifactSource(tsf)
    val ops = new DefaultRugPipeline(DefaultTypeRegistry).create(as, None)
    val red = ops.head.asInstanceOf[RugDrivenProjectEditor]
    red.name should be("TwoStepEditor")
    red.setContext(others)

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("README", "I dub thee... Flounder"))

    val p = SimpleProjectOperationArguments("", Map[String, Object]())
    red.modify(target, p) match {
      case sm: SuccessfulModification =>
        sm.result.totalFileCount should be(2)
        sm.result.findFile("DoubleSecretFile").get.content.contains("Probation") should be(true)
        sm.result.findFile("README").get.content.contains("Flounder") should be(true)
        sm.result.findFile("README").get.content.contains("Pledge") should be(false)
      case _ => fail("two identical modifications reported no modifications")
    }
  }
}


class RugExecutorIsNoLongerSupportedTest extends FlatSpec with Matchers {

  it should "Give a great error message if someone has an old executor in Rug DSL" in {
    val rug =
      """executor FriendlyBanana
        |blah blah whatever
      """.stripMargin
    val tsf = StringFileArtifact(".atomist/editors/FriendlyBanana.rug", rug)
    val as = SimpleFileBasedArtifactSource(tsf)
    try {
      new DefaultRugPipeline(DefaultTypeRegistry).create(as, None)
      fail("that should fail")
    } catch {
      case bre: BadRugException => bre.getMessage should be("The Rug DSL no longer supports executors. Try writing it in TypeScript!")
    }
  }

}

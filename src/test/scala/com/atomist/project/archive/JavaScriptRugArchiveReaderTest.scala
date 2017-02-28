package com.atomist.project.archive

import com.atomist.param.{ParameterValues, SimpleParameterValues, Tag}
import com.atomist.project.ProjectOperation
import com.atomist.project.edit._
import com.atomist.rug.SimpleJavaScriptProjectOperationFinder
import com.atomist.rug.runtime.AddressableRug
import com.atomist.rug.runtime.js.JavaScriptProjectEditor
import com.atomist.rug.runtime.lang.js.NashornConstructorTest
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.file.ClassPathArtifactSource
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class JavaScriptRugArchiveReaderTest extends FlatSpec with Matchers {


  it should "load handlers of different kinds from an archive" in {
    val ts = ClassPathArtifactSource.toArtifactSource("com/atomist/project/archive/MyHandlers.ts")
    val moved = ts.withPathAbove(".atomist/handlers")
    val as = TypeScriptBuilder.compileWithModel(moved)
    val reader = new JavaScriptRugArchiveReader()
    val ops = reader.find(as, Nil)
    assert(ops.responseHandlers.size === 2)
    assert(ops.commandHandlers.size === 2)
    assert(ops.eventHandlers.size === 1)
  }
  it should "find and invoke plain javascript generators" in {
    val apc =  new JavaScriptRugArchiveReader()
    val f1 = StringFileArtifact("package.json", "{}")
    val f2 = StringFileArtifact("app/Thing.js", "var Thing = {};")

    val rugAs = SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/SimpleEditor.js",
        NashornConstructorTest.SimpleJavascriptEditor),
      f1,
      f2
    ) + TypeScriptBuilder.userModel

    val ops = apc.find(rugAs, Nil)
    assert(ops.editors.size === 1)
    assert(ops.editors.head.parameters.size === 1)
  }

  val SimpleEditorInvokingOtherEditor =
    """
      |import {Project} from '@atomist/rug/model/Core'
      |import {Editor} from '@atomist/rug/operations/Decorators'
      |
      |@Editor("Simple", "My simple editor")
      |class SimpleEditor {
      |    edit(project: Project) {
      |        project.editWith("other", { otherParam: "Anders Hjelsberg is God" });
      |    }
      |}
      |export let myeditor = new SimpleEditor()

    """.stripMargin

  val SimpleEditorInvokingOtherEditorViaGa =
    """
      |import {Project} from '@atomist/rug/model/Core'
      |import {Editor} from '@atomist/rug/operations/Decorators'
      |
      |@Editor("Simple", "My simple editor")
      |class SimpleEditor {
      |    edit(project: Project) {
      |        project.editWith("com.atomist.rugs:common-rugs:other", { otherParam: "Anders Hjelsberg is God" });
      |    }
      |}
      |export let myeditor = new SimpleEditor()

    """.stripMargin

  val OtherEditorProg  =
    """
      |import {Project} from '@atomist/rug/model/Core'
      |import {Editor} from '@atomist/rug/operations/Decorators'
      |
      |@Editor("other", "Another editor")
      |class OtherEditor {
      |    edit(project: Project) {
      |       project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |    }
      |}
      |export let otherEditor = new OtherEditor()

    """.stripMargin

  val otherEditor: AddressableRug = new ProjectEditorSupport with AddressableRug{
    override protected  def modifyInternal(as: ArtifactSource, pmi: ParameterValues): ModificationAttempt = {
      SuccessfulModification(as + StringFileArtifact("src/from/typescript", pmi.stringParamValue("otherParam")))
    }
    override def applicability(as: ArtifactSource): Applicability = Applicability.OK
    override def name: String = "other"
    override def description: String = name
    override def tags: Seq[Tag] = Nil

    override def artifact: String = "common-rugs"

    override def group: String = "com.atomist.rugs"

    override def version: String = "1.2.3"
  }

  it should "invoke editors in the same archive" in {
    invokeAndVerifySimple(
      Seq(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorInvokingOtherEditor),
        StringFileArtifact(s".atomist/editors/Other.ts", OtherEditorProg)
    ), Nil)
  }

  it should "invoke editors in a different archive referenced by GAV" in {
    invokeAndVerifySimple(
      Seq(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorInvokingOtherEditorViaGa)
      ), Seq(otherEditor))
  }

  it should "invoke editors in a different archive referenced by simple name as fallback" in {
    invokeAndVerifySimple(
      Seq(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorInvokingOtherEditor)
      ), Seq(otherEditor))
  }

  private  def invokeAndVerifySimple(tsf: Seq[FileArtifact], others: Seq[AddressableRug] = Nil): ProjectEditor = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf:_*))
    val reader = new JavaScriptRugArchiveReader()
    val jsed = reader.find(as, others).editors.head

    assert(jsed.name === "Simple")

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    jsed.modify(target, SimpleParameterValues(Map("content" -> "Anders Hjelsberg is God"))) match {
      case sm: SuccessfulModification =>
        assert(sm.result.totalFileCount === 2)
        sm.result.findFile("src/from/typescript").get.content.contains("Anders") should be(true)
      case _ => ???
    }
    jsed
  }
}

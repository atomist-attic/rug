package com.atomist.project.archive

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit._
import com.atomist.rug.runtime.js.nashorn.NashornConstructorTest
import com.atomist.rug.{DuplicateRugException, RugArchiveReader}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.file.ClassPathArtifactSource
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class RugArchiveReaderTest extends FlatSpec with Matchers {

  it should "load handlers of different kinds from an archive" in {
    val ts = ClassPathArtifactSource.toArtifactSource(
      "com/atomist/project/archive/MyHandlers.ts")
    val moved = ts.withPathAbove(".atomist/handlers")
    val as = TypeScriptBuilder.compileWithModel(moved)
    val ops = RugArchiveReader(as)
    assert(ops.responseHandlers.size === 2)
    assert(ops.commandHandlers.size === 2)
    assert(ops.eventHandlers.size === 1)
  }

  it should "find and invoke plain javascript editors" in {
    val f1 = StringFileArtifact("package.json", "{}")
    val f2 = StringFileArtifact("app/Thing.js", "var Thing = {};")

    val rugAs = SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/SimpleEditor.js",
        NashornConstructorTest.SimpleJavascriptEditor),
      f1,
      f2
    ) + TypeScriptBuilder.userModel

    val ops = RugArchiveReader(rugAs)
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

  it should "invoke editors in the same archive" in {
    invokeAndVerifySimple(
      Seq(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorInvokingOtherEditor),
        StringFileArtifact(s".atomist/editors/Other.ts", OtherEditorProg)
    ))
  }

  it should "invoke editors in a different archive referenced by GAV" in {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(StringFileArtifact(s".atomist/editors/Other.ts", OtherEditorProg)))
    val coords = Coordinate("com.atomist.rugs","common-rugs", "1.2.3")
    val deps = Seq(Dependency(as, Some(coords), Nil))

    invokeAndVerifySimple(
      Seq(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorInvokingOtherEditorViaGa)
      ), deps)
  }

  it should "invoke editors in a different archive referenced by simple name as fallback" in {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(StringFileArtifact(s".atomist/editors/Other.ts", OtherEditorProg)))
    val coords = Coordinate("com.atomist.rugs","common-rugs", "1.2.3")
    val deps = Seq(Dependency(as, Some(coords), Nil))

    invokeAndVerifySimple(
      Seq(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorInvokingOtherEditor)
      ), deps)
  }

  it should "throw exceptions if there are duplicate rugs in an archive https://github.com/atomist/rug/issues/561" in {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(
      StringFileArtifact(s".atomist/editors/Other.ts", SimpleEditorInvokingOtherEditor),
      StringFileArtifact(s".atomist/editors/Other2.ts", SimpleEditorInvokingOtherEditorViaGa)))
    val coords = Coordinate("com.atomist.rugs","common-rugs", "1.2.3")
    val deps = Seq(Dependency(as, Some(coords), Nil))

    assertThrows[DuplicateRugException]{
      invokeAndVerifySimple(
        Seq(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorInvokingOtherEditor)
        ), deps)
    }
  }

  private  def invokeAndVerifySimple(tsf: Seq[FileArtifact], dependencies: Seq[Dependency] = Nil): ProjectEditor = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf:_*))
    val coord = Coordinate("com.atomist.test","test-rugs", "1.2.3")
    val resolver =  new ArchiveRugResolver(Dependency(as, Some(coord), dependencies))
    val jsed = resolver.resolvedDependencies.rugs.editors.head

    assert(jsed.name === "Simple")

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    jsed.modify(target, SimpleParameterValues(Map("content" -> "Anders Hjelsberg is God"))) match {
      case sm: SuccessfulModification =>
        assert(sm.result.totalFileCount === 2)
        sm.result.findFile("src/from/typescript").get.content.contains("Anders") shouldBe true
      case _ => ???
    }
    jsed
  }
}

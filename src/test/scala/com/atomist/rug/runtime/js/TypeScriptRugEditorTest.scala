package com.atomist.rug.runtime.js

import com.atomist.project.common.IllformedParametersException
import com.atomist.project.edit._
import com.atomist.project.{ProjectOperation, ProjectOperationArguments, SimpleProjectOperationArguments}
import com.atomist.rug.TestUtils
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object TypeScriptRugEditorTest {

  val ContentPattern = "^Anders .*$"

  val compiler = new TypeScriptCompiler()

  val SimpleEditorWithoutParameters =
    """
      |import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {Result,Status} from '@atomist/rug/operations/RugOperation'
      |
      |class SimpleEditor implements ProjectEditor {
      |    name: string = "Simple"
      |    description: string = "My simple editor"
      |    edit(project: Project):Result {
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        return new Result(Status.Success,
      |         `Edited Project now containing ${project.fileCount()} files: \n`)
      |    }
      |}
      |var editor = new SimpleEditor()
    """.stripMargin

  val SimpleEditor =
    """
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {Project} from '@atomist/rug/model/Core'
      |import {Result,Status} from '@atomist/rug/operations/RugOperation'
      |
      |class SimpleEditor implements ProjectEditor {
      |    name: string = "Simple"
      |    description: string = "My simple editor"
      |    edit(project: Project):Result {
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        return new Result(Status.Success,
      |         `Edited Project now containing ${project.fileCount()} files: \n`)
      |    }
      |}
      |
      |var editor = new SimpleEditor()
    """.stripMargin

  val SimpleEditorInvokingOtherEditor =
    """
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {Project} from '@atomist/rug/model/Core'
      |import {Result,Status} from '@atomist/rug/operations/RugOperation'
      |
      |class SimpleEditor implements ProjectEditor {
      |    name: string = "Simple"
      |    description: string = "My simple editor"
      |    edit(project: Project) {
      |        project.editWith("other", { otherParam: "Anders Hjelsberg is God" });
      |        return new Result(Status.Success,
      |         `Edited Project now containing ${project.fileCount()} files: \n`)
      |    }
      |}
      |var editor = new SimpleEditor()

    """.stripMargin

  val SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
       |import {File} from '@atomist/rug/model/Core'
       |import {Result,Status, Parameter} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleEditor implements ProjectEditor {
       |    name: string = "Simple"
       |    description: string = "My simple editor"
       |    tags: string[] = ["java", "maven"]
       |    parameters: Parameter[] = [{name: "content", description: "Content", displayName: "content", pattern: "$ContentPattern", maxLength: 100}]
       |    edit(project: Project, {content} : {content: string}) {
       |      //TODO p["otherParam"] = p.content
       |      project.editWith("other", { otherParam: "Anders Hjelsberg is God" })
       |      return new Result(Status.Success,
       |        `Edited Project now containing $${project.fileCount()} files: \n`
       |        );
       |    }
       |  }
       |var editor = new SimpleEditor()
    """.stripMargin

  val SimpleGenerator =
    """
      |import {ProjectGenerator} from '@atomist/rug/operations/ProjectGenerator'
      |import {Project} from '@atomist/rug/model/Core'
      |import {Status,Result} from '@atomist/rug/operations/RugOperation'
      |
      |
      |class SimpleGenerator implements ProjectGenerator{
      |     description: string = "My simple Generator"
      |     name: string = "SimpleGenerator"
      |     populate(project: Project, projectName: string) {
      |        project.copyEditorBackingFilesPreservingPath("")
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        return new Result(Status.Success, `Edited Project now containing ${project.fileCount()} files: \n`)
      |    }
      |}
      |var gen = new SimpleGenerator()
    """.stripMargin

  val SimpleEditorTaggedAndMeta =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
       |import {Parameter, Result, Status} from '@atomist/rug/operations/RugOperation'
       |import {File} from '@atomist/rug/model/Core'
       |
       |class SimpleEditor implements ProjectEditor {
       |
       |    name: string = "Simple"
       |    description: string = "A nice little editor"
       |    tags: string[] = ["java", "maven"]
       |    parameters: Parameter[] = [
       |        {name: "content", description: "Content", displayName: "content", pattern: "$ContentPattern", maxLength: 100, default: "Anders"},
       |        {name: "num", description: "some num", displayName: "num", pattern: "^[\\d]+$$", maxLength: 100, default: 10}
       |    ]
       |
       |    edit(project: Project, {content, num }: {content: string, num: number}): Result {
       |      project.addFile("src/from/typescript", content);
       |      return new Result(Status.Success,
       |      `Edited Project now containing $${project.fileCount()} files: \n`);
       |    }
       |  }
       |var myeditor = new SimpleEditor()
    """.stripMargin

    val EditorInjectedWithPathExpressionObject: String =
      """import {Project} from '@atomist/rug/model/Core'
        |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
        |import {PathExpression} from '@atomist/rug/tree/PathExpression'
        |import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
        |import {Match} from '@atomist/rug/tree/PathExpression'
        |import {File} from '@atomist/rug/model/Core'
        |import {Result,Status, Parameter} from '@atomist/rug/operations/RugOperation'
        |
        |class PomFile extends PathExpression<Project,File> {
        |
        |    constructor() { super(`/File()[@name='pom.xml']`) }
        |}
        |
        |class ConstructedEditor implements ProjectEditor {
        |
        |    name: string = "Constructed"
        |    description: string = "A nice little editor"
        |    tags: string[] = ["java", "maven"]
        |    parameters: Parameter[] = [{name: "packageName", description: "The Java package name", displayName: "Java Package", pattern: "^.*$", maxLength: 100}]
        |    edit(project: Project, {packageName } : {packageName: string}) {
        |
        |      let eng: PathExpressionEngine = project.context().pathExpressionEngine();
        |      let m = eng.evaluate<Project,File>(project, new PomFile())
        |
        |      var t: string = `param=${packageName},filecount=${m.root().fileCount()}`
        |      for (let n of m.matches()) {
        |        t += `Matched file=${n.path()}`;
        |        n.append("randomness")
        |        }
        |
        |        var s: string = ""
        |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
        |        for (let f of project.files())
        |            s = s + `File [${f.path()}] containing [${f.content()}]\n`
        |        return new Result(Status.Success,
        |        `${t}\n\nEdited Project containing ${project.fileCount()} files: \n${s}`)
        |    }
        |  }
        |  var editor = new ConstructedEditor()
        | """.stripMargin

  val EditorInjectedWithPathExpression: String =
    """import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {PathExpression} from '@atomist/rug/tree/PathExpression'
      |import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
      |import {Match} from '@atomist/rug/tree/PathExpression'
      |import {File} from '@atomist/rug/model/Core'
      |import {Result,Status, Parameter} from '@atomist/rug/operations/RugOperation'
      |
      |class ConstructedEditor implements ProjectEditor {
      |
      |    name: string = "Constructed"
      |    description: string = "A nice little editor"
      |    tags: string[] = ["java", "maven"]
      |    parameters: Parameter[] = [{name: "packageName", description: "The Java package name", displayName: "Java Package", pattern: "^.*$", maxLength: 100}]
      |    edit(project: Project, {packageName } : {packageName: string}) {
      |
      |      let eng: PathExpressionEngine = project.context().pathExpressionEngine();
      |      let pe = new PathExpression<Project,File>(`/File()[@name='pom.xml']`)
      |      //console.log(pe.expression);
      |      let m: Match<Project,File> = eng.evaluate(project, pe)
      |
      |      var t: string = `param=${packageName},filecount=${m.root().fileCount()}`
      |      for (let n of m.matches()) {
      |        t += `Matched file=${n.path()}`;
      |        n.append("randomness")
      |        }
      |
      |        var s: string = ""
      |
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        for (let f of project.files())
      |            s = s + `File [${f.path()}] containing [${f.content()}]\n`
      |        return new Result(Status.Success,
      |        `${t}\n\nEdited Project containing ${project.fileCount()} files: \n${s}`)
      |    }
      |  }
      |  var editor = new ConstructedEditor()
      | """.stripMargin

  val EditorInjectedWithPathExpressionUsingWith: String =
    """import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {PathExpression} from '@atomist/rug/tree/PathExpression'
      |import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
      |import {Match} from '@atomist/rug/tree/PathExpression'
      |import {File} from '@atomist/rug/model/Core'
      |import {Result,Status, Parameter} from '@atomist/rug/operations/RugOperation'
      |
      |class ConstructedEditor implements ProjectEditor {
      |
      |    name: string = "Constructed"
      |    description: string = "A nice little editor"
      |    tags: string[] = ["java", "maven"]
      |    parameters: Parameter[] = [{name: "packageName", description: "The Java package name", displayName: "Java Package", pattern: "^.*$", maxLength: 100}]
      |
      |    edit(project: Project, {packageName } : {packageName: string}) {
      |      let eng: PathExpressionEngine = project.context().pathExpressionEngine();
      |      project.files().filter(t => false)
      |      var t: string = `param=${packageName},filecount=${project.fileCount()}`
      |
      |      eng.with<File>(project, "/*[@name='pom.xml']", n => {
      |        t += `Matched file=${n.path()}`;
      |        n.append("randomness")
      |      })
      |
      |        var s: string = ""
      |
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        for (let f of project.files())
      |            s = s + `File [${f.path()}] containing [${f.content()}]\n`
      |        return new Result(Status.Success,
      |        `${t}\n\nEdited Project containing ${project.fileCount()} files: \n${s}`)
      |    }
      |  }
      |
      |  var editor = new ConstructedEditor()
      | """.stripMargin

  val EditorInjectedWithPathExpressionUsingWithTypeJump: String =
    """import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {PathExpression} from '@atomist/rug/tree/PathExpression'
      |import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
      |import {Match} from '@atomist/rug/tree/PathExpression'
      |import {File} from '@atomist/rug/model/Core'
      |import {Result,Status, Parameter} from '@atomist/rug/operations/RugOperation'
      |
      |class ConstructedEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "A nice little editor"
      |    tags: string[] = ["java", "maven"]
      |
      |    parameters: Parameter[] = [{name: "packageName", description: "The Java package name", displayName: "Java Package", pattern: "^.*$", maxLength: 100}]
      |    edit(project: Project, {packageName } : { packageName: string}) {
      |
      |      let eng: PathExpressionEngine = project.context().pathExpressionEngine();
      |
      |      var t: string = `param=${packageName},filecount=${project.fileCount()}`
      |
      |      eng.with<File>(project, "/File()", n => {
      |        t += `Matched file=${n.path()}`;
      |        n.append("randomness")
      |      })
      |
      |        var s: string = ""
      |
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        for (let f of project.files())
      |            s = s + `File [${f.path()}] containing [${f.content()}]\n`
      |        return new Result(Status.Success,
      |        `${t}\n\nEdited Project containing ${project.fileCount()} files: \n${s}`)
      |    }
      |  }
      |  var editor = new ConstructedEditor()
      | """.stripMargin

}

class TypeScriptRugEditorTest extends FlatSpec with Matchers {

  import TypeScriptRugEditorTest._

  it should "run simple editor compiled from TypeScript without parameters using support class" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorWithoutParameters))
  }

  it should "run simple editor twice and see no change the second time" in {
    invokeAndVerifyIdempotentSimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorWithoutParameters))
  }

  it should "run simple editor compiled from TypeScript" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditor))
  }

  it should "run simple editor compiled from TypeScript that invokes another editor with separate parameters object" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorInvokingOtherEditor), Seq(otherEditor))
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

  it should "run simple editor compiled from TypeScript that invokes another editor adding to our parameters object" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters), Seq(otherEditor))
  }

  it should "find tags" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      SimpleEditorTaggedAndMeta))
    ed.tags.size should be(2)
    ed.tags.map(_.name).toSet should equal(Set("java", "maven"))
  }

  it should "find parameter metadata" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      SimpleEditorTaggedAndMeta))
    ed.parameters.size should be(2)
    val p = ed.parameters.head
    p.name should be("content")
    p.description should be("Content")
    p.getDisplayName should be("content")
    p.getPattern should be(ContentPattern)
    p.getMaxLength should be(100)
  }

  it should "find description" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      SimpleEditorTaggedAndMeta))
    ed.description should be("A nice little editor")
  }

  it should "have the PathExpressionEngine injected" in {
    val ed = invokeAndVerifyConstructed(StringFileArtifact(s".atomist/editors/ConstructedEditor.ts",
      EditorInjectedWithPathExpression))
    ed.description should be ("A nice little editor")
  }

  it should "have the PathExpressionEngine injected and use an object path expression" in {
    val ed = invokeAndVerifyConstructed(StringFileArtifact(s".atomist/editors/ConstructedEditor.ts",
      EditorInjectedWithPathExpressionObject))
    ed.description should be ("A nice little editor")
  }

  it should "have the PathExpressionEngine injected using PathExpressionEngine.with" in {
    val ed = invokeAndVerifyConstructed(StringFileArtifact(s".atomist/editors/ConstructedEditor.ts",
      EditorInjectedWithPathExpressionUsingWith))
    ed.description should be ("A nice little editor")
  }

  it should "have the PathExpressionEngine injected using PathExpressionEngine.with type-jump" in {
    val ed = invokeAndVerifyConstructed(StringFileArtifact(s".atomist/editors/ConstructedEditor.ts",
      EditorInjectedWithPathExpressionUsingWithTypeJump))
  }

  it should "send editor bad input and get appropriate response" in {
    val as = SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/Simple.ts", SimpleEditorTaggedAndMeta))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(TestUtils.compileWithModel(as)).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    jsed.name should be("Simple")

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    // This should not work beause it doesn't meet the content pattern
    an[IllformedParametersException] should be thrownBy (jsed.modify(target, SimpleProjectOperationArguments("", Map("content" -> "Bjarn Stroustrup is God"))))
  }

  it should "handle default parameter values" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      SimpleEditorTaggedAndMeta))
    ed.parameters.size should be(2)
    val p = ed.parameters.head
    p.name should be("content")
    p.description should be("Content")
    p.getDisplayName should be("content")
    p.getPattern should be(ContentPattern)
    p.getDefaultValue should be("Anders")
    p.getMaxLength should be(100)
  }

  private def invokeAndVerifyConstructed(tsf: FileArtifact): JavaScriptInvokingProjectEditor = {
    val as = SimpleFileBasedArtifactSource(tsf)
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(TestUtils.compileWithModel(as)).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    jsed.name should be("Constructed")
    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
    jsed.modify(target, SimpleProjectOperationArguments("", Map("packageName" -> "com.atomist.crushed"))) match {
      case sm: SuccessfulModification =>
      //sm.comment.contains("OK") should be(true)
        sm.result.findFile("pom.xml").get.content.contains("randomness") should be (true)
    }
    jsed
  }

  private def invokeAndVerifySimple(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil): JavaScriptInvokingProjectEditor = {
    val as = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(tsf))

    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    jsed.name should be("Simple")
    jsed.setContext(others)

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    jsed.modify(target, SimpleProjectOperationArguments("", Map("content" -> "Anders Hjelsberg is God"))) match {
      case sm: SuccessfulModification =>
        sm.result.totalFileCount should be(2)
        sm.result.findFile("src/from/typescript").get.content.contains("Anders") should be(true)
    }
    jsed
  }

  private def invokeAndVerifyIdempotentSimple(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil): JavaScriptInvokingProjectEditor = {
    val as = SimpleFileBasedArtifactSource(tsf)
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(TestUtils.compileWithModel(as)).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    jsed.name should be("Simple")
    jsed.setContext(others)

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    val p = SimpleProjectOperationArguments("", Map("content" -> "Anders Hjelsberg is God"))
    jsed.modify(target, p) match {
      case sm: SuccessfulModification =>
        sm.result.totalFileCount should be(2)
        sm.result.findFile("src/from/typescript").get.content.contains("Anders") should be(true)

        jsed.modify(sm.result, p) match {
          case _: NoModificationNeeded => //yay
          case sm: SuccessfulModification =>
              fail("That should not have reported modification")
        }
    }
    jsed
  }
}

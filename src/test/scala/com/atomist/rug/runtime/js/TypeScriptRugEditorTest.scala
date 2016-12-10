package com.atomist.rug.runtime.js

import com.atomist.project.common.IllformedParametersException
import com.atomist.project.edit._
import com.atomist.project.{ProjectOperation, ProjectOperationArguments, SimpleProjectOperationArguments}
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object TypeScriptRugEditorTest {

  val ContentPattern = "Anders .*"

  val SimpleEditorWithoutParameters =
    """
      |import {ParameterlessProjectEditor} from 'user-model/operations/ProjectEditor'
      |import {Parameters} from 'user-model/operations/Parameters'
      |import {Project} from 'user-model/model/Core'
      |import {editor} from 'user-model/support/Metadata'
      |import {Result,Status} from 'user-model/operations/Result'
      |
      |@editor("My simple editor")
      |class SimpleEditor extends ParameterlessProjectEditor {
      |
      |    editWithoutParameters(project: Project):Result {
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        return new Result(Status.Success,
      |         `Edited Project now containing ${project.fileCount()} files: \n`)
      |    }
      |}
    """.stripMargin

  val SimpleEditor =
    """
      |import {ProjectEditor} from 'user-model/operations/ProjectEditor'
      |import {Parameters} from 'user-model/operations/Parameters'
      |import {Project} from 'user-model/model/Core'
      |import {editor} from 'user-model/support/Metadata'
      |import {parameters} from 'user-model/support/Metadata'
      |import {Result,Status} from 'user-model/operations/Result'
      |
      |@editor("My simple editor")
      |class SimpleEditor implements ProjectEditor<Parameters> {
      |
      |    edit(project: Project, @parameters("Parameters") p: Parameters):Result {
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        return new Result(Status.Success,
      |         `Edited Project now containing ${project.fileCount()} files: \n`)
      |    }
      |}
    """.stripMargin

  val SimpleEditorInvokingOtherEditor =
    """
      |import {ProjectEditor} from 'user-model/operations/ProjectEditor'
      |import {Parameters} from 'user-model/operations/Parameters'
      |import {Project} from 'user-model/model/Core'
      |import {editor} from 'user-model/support/Metadata'
      |import {parameters} from 'user-model/support/Metadata'
      |
      |@editor("My simple editor")
      |class SimpleEditor implements ProjectEditor<Parameters> {
      |
      |    edit(project: Project, @parameters("Parameters") p: Parameters) {
      |        project.editWith("other", { otherParam: "Anders Hjelsberg is God" });
      |        return "thing"
      |    }
      |}
    """.stripMargin

  val SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters =
    s"""
       |import {Project} from 'user-model/model/Core'
       |import {ParametersSupport} from 'user-model/operations/Parameters'
       |import {ProjectEditor} from 'user-model/operations/ProjectEditor'
       |import {Parameters} from 'user-model/operations/Parameters'
       |import {File} from 'user-model/model/Core'
       |import {Result,Status} from 'user-model/operations/Result'
       |
       |import {parameter} from 'user-model/support/Metadata'
       |import {inject} from 'user-model/support/Metadata'
       |import {parameters} from 'user-model/support/Metadata'
       |import {tag} from 'user-model/support/Metadata'
       |import {editor} from 'user-model/support/Metadata'
       |
       |abstract class ContentInfo extends ParametersSupport {
       |
       |  @parameter({description: "Content", displayName: "content", pattern: "$ContentPattern", maxLength: 100})
       |  content: string = null
       |
       |}
       |
       |@editor("A nice little editor")
       |@tag("java")
       |@tag("maven")
       |class SimpleEditor implements ProjectEditor<ContentInfo> {
       |
       |    edit(project: Project, @parameters("ContentInfo") p: ContentInfo) {
       |      p["otherParam"] = p.content
       |      project.editWith("other", p)
       |      return new Result(Status.Success,
       |        `Edited Project now containing $${project.fileCount()} files: \n`
       |        );
       |    }
       |  }
       |
    """.stripMargin

  val SimpleGenerator =
    """
      |import {ProjectEditor} from 'user-model/operations/ProjectEditor'
      |import {GeneratorParameters} from 'user-model/operations/ProjectGenerator'
      |import {ProjectGenerator} from 'user-model/operations/ProjectGenerator'
      |import {Project} from 'user-model/model/Core'
      |import {Status,Result} from 'user-model/operations/Result'
      |
      |import {generator} from 'user-model/support/Metadata'
      |import {parameters} from 'user-model/support/Metadata'
      |
      |@generator("My simple Generator")
      |class SimpleGenerator implements ProjectGenerator<GeneratorParameters> {
      |
      |     populate(project: Project, parameters: GeneratorParameters) {
      |        project.copyEditorBackingFilesPreservingPath("")
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        return new Result(Status.Success, `Edited Project now containing ${project.fileCount()} files: \n`)
      |    }
      |}
    """.stripMargin

  val SimpleEditorTaggedAndMeta =
    s"""
       |import {Project} from 'user-model/model/Core'
       |import {ParametersSupport} from 'user-model/operations/Parameters'
       |import {ProjectEditor} from 'user-model/operations/ProjectEditor'
       |import {Parameters} from 'user-model/operations/Parameters'
       |import {File} from 'user-model/model/Core'
       |import {Result,Status} from 'user-model/operations/Result'
       |
       |import {parameter} from 'user-model/support/Metadata'
       |import {inject} from 'user-model/support/Metadata'
       |import {parameters} from 'user-model/support/Metadata'
       |import {tag} from 'user-model/support/Metadata'
       |import {editor} from 'user-model/support/Metadata'
       |
       |abstract class ContentInfo extends ParametersSupport {
       |
       |  @parameter({description: "Content", displayName: "content", pattern: "$ContentPattern", maxLength: 100})
       |  content: string = "Anders"
       |
       |   @parameter({description: "some num", displayName: "num", pattern: "[\\d]+", maxLength: 100})
       |   num: number = 10
       |}
       |
       |@editor("A nice little editor")
       |@tag("java")
       |@tag("maven")
       |class SimpleEditor implements ProjectEditor<ContentInfo> {
       |
       |    edit(project: Project, @parameters("ContentInfo") p: ContentInfo): Result {
       |      project.addFile("src/from/typescript", p.content);
       |      return new Result(Status.Success,
       |      `Edited Project now containing $${project.fileCount()} files: \n`);
       |    }
       |  }
       |
    """.stripMargin

  val EditorInjectedWithPathExpression =
    """import {Project} from 'user-model/model/Core'
      |import {ParametersSupport} from 'user-model/operations/Parameters'
      |import {ProjectEditor} from 'user-model/operations/ProjectEditor'
      |import {Parameters} from 'user-model/operations/Parameters'
      |import {PathExpression} from 'user-model/tree/PathExpression'
      |import {PathExpressionEngine} from 'user-model/tree/PathExpression'
      |import {Match} from 'user-model/tree/PathExpression'
      |import {File} from 'user-model/model/Core'
      |import {Result,Status} from 'user-model/operations/Result'
      |
      |import {parameter} from 'user-model/support/Metadata'
      |import {inject} from 'user-model/support/Metadata'
      |import {parameters} from 'user-model/support/Metadata'
      |import {tag} from 'user-model/support/Metadata'
      |import {editor} from 'user-model/support/Metadata'
      |
      |abstract class JavaInfo extends ParametersSupport {
      |
      |  @parameter({description: "The Java package name", displayName: "Java Package", pattern: ".*", maxLength: 100})
      |  packageName: string = null
      |
      |}
      |
      |@editor("A nice little editor")
      |@tag("java")
      |@tag("maven")
      |class ConstructedEditor implements ProjectEditor<Parameters> {
      |
      |    private eng: PathExpressionEngine;
      |
      |    constructor(@inject("PathExpressionEngine") _eng: PathExpressionEngine ){
      |      this.eng = _eng;
      |    }
      |    edit(project: Project, @parameters("JavaInfo") ji: JavaInfo) {
      |
      |      let pe = new PathExpression<Project,File>(`/*:file[name='pom.xml']`)
      |      //console.log(pe.expression);
      |      let m: Match<Project,File> = this.eng.evaluate(project, pe)
      |
      |      ji["whatever"] = "thing"
      |
      |      var t: string = `param=${ji.packageName},filecount=${m.root().fileCount()}`
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
      | """.stripMargin

  val EditorInjectedWithPathExpressionUsingWith =
    """import {Project} from 'user-model/model/Core'
      |import {ParametersSupport} from 'user-model/operations/Parameters'
      |import {ProjectEditor} from 'user-model/operations/ProjectEditor'
      |import {Parameters} from 'user-model/operations/Parameters'
      |import {PathExpression} from 'user-model/tree/PathExpression'
      |import {PathExpressionEngine} from 'user-model/tree/PathExpression'
      |import {Match} from 'user-model/tree/PathExpression'
      |import {File} from 'user-model/model/Core'
      |import {Result,Status} from 'user-model/operations/Result'
      |
      |import {parameter} from 'user-model/support/Metadata'
      |import {inject} from 'user-model/support/Metadata'
      |import {parameters} from 'user-model/support/Metadata'
      |import {tag} from 'user-model/support/Metadata'
      |import {editor} from 'user-model/support/Metadata'
      |
      |abstract class JavaInfo extends ParametersSupport {
      |
      |  @parameter({description: "The Java package name", displayName: "Java Package", pattern: ".*", maxLength: 100})
      |  packageName: string = null
      |
      |}
      |
      |@editor("A nice little editor")
      |@tag("java")
      |@tag("maven")
      |class ConstructedEditor implements ProjectEditor<Parameters> {
      |
      |    private eng: PathExpressionEngine;
      |
      |    constructor(@inject("PathExpressionEngine") _eng: PathExpressionEngine ){
      |      this.eng = _eng;
      |    }
      |
      |    edit(project: Project, @parameters("JavaInfo") ji: JavaInfo) {
      |
      |      project.files().filter(t => false)
      |      var t: string = `param=${ji.packageName},filecount=${project.fileCount()}`
      |
      |      this.eng.with<File>(project, "/*:file[name='pom.xml']", n => {
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
      | """.stripMargin

  val EditorInjectedWithPathExpressionUsingWithTypeJump =
    """import {Project} from 'user-model/model/Core'
      |import {ParametersSupport} from 'user-model/operations/Parameters'
      |import {ProjectEditor} from 'user-model/operations/ProjectEditor'
      |import {Parameters} from 'user-model/operations/Parameters'
      |import {PathExpression} from 'user-model/tree/PathExpression'
      |import {PathExpressionEngine} from 'user-model/tree/PathExpression'
      |import {Match} from 'user-model/tree/PathExpression'
      |import {File} from 'user-model/model/Core'
      |import {Result,Status} from 'user-model/operations/Result'
      |
      |import {parameter} from 'user-model/support/Metadata'
      |import {inject} from 'user-model/support/Metadata'
      |import {parameters} from 'user-model/support/Metadata'
      |import {tag} from 'user-model/support/Metadata'
      |import {editor} from 'user-model/support/Metadata'
      |
      |abstract class JavaInfo extends ParametersSupport {
      |
      |  @parameter({description: "The Java package name", displayName: "Java Package", pattern: ".*", maxLength: 100})
      |  packageName: string = null
      |
      |}
      |
      |@editor("A nice little editor")
      |@tag("java")
      |@tag("maven")
      |class ConstructedEditor implements ProjectEditor<Parameters> {
      |
      |    private eng: PathExpressionEngine;
      |
      |    constructor(@inject("PathExpressionEngine") _eng: PathExpressionEngine ){
      |      this.eng = _eng;
      |    }
      |
      |    edit(project: Project, @parameters("JavaInfo") ji: JavaInfo) {
      |
      |      var t: string = `param=${ji.packageName},filecount=${project.fileCount()}`
      |
      |      this.eng.with<File>(project, "->file", n => {
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
      | """.stripMargin

}

class TypeScriptRugEditorTest extends FlatSpec with Matchers {

  import TypeScriptRugEditorTest._

  it should "run simple editor compiled from TypeScript without parameters using support class" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/SimpleEditor.ts", SimpleEditorWithoutParameters))
  }

  it should "run simple editor twice and see no change the second time" in {
    invokeAndVerifyIdempotentSimple(StringFileArtifact(s".atomist/SimpleEditor.ts", SimpleEditorWithoutParameters))
  }

  it should "run simple editor compiled from TypeScript" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/SimpleEditor.ts", SimpleEditor))
  }

  it should "run simple editor compiled from TypeScript that invokes another editor with separate parameters object" in pendingUntilFixed {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/SimpleEditor.ts", SimpleEditorInvokingOtherEditor), Seq(otherEditor))
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
    invokeAndVerifySimple(StringFileArtifact(s".atomist/SimpleEditor.ts",
      SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters), Seq(otherEditor))
  }

  it should "find tags" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/SimpleEditor.ts",
      SimpleEditorTaggedAndMeta))
    ed.tags.size should be(2)
    ed.tags.map(_.name).toSet should equal(Set("java", "maven"))
  }

  it should "find parameter metadata" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/SimpleEditor.ts",
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
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/SimpleEditor.ts",
      SimpleEditorTaggedAndMeta))
    ed.description should be("A nice little editor")
  }

  it should "have the PathExpressionEngine injected" in {
    val ed = invokeAndVerifyConstructed(StringFileArtifact(s".atomist/ConstructedEditor.ts",
      EditorInjectedWithPathExpression))
    ed.description should be ("A nice little editor")
  }

  it should "have the PathExpressionEngine injected using PathExpressionEngine.with" in {
    val ed = invokeAndVerifyConstructed(StringFileArtifact(s".atomist/ConstructedEditor.ts",
      EditorInjectedWithPathExpressionUsingWith))
    ed.description should be ("A nice little editor")
  }

  it should "have the PathExpressionEngine injected using PathExpressionEngine.with typejump" in {
    val ed = invokeAndVerifyConstructed(StringFileArtifact(s".atomist/ConstructedEditor.ts",
      EditorInjectedWithPathExpressionUsingWithTypeJump))
  }

  it should "send editor bad input and get appropriate response" in {
    val as = SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/Simple.ts", SimpleEditorTaggedAndMeta))
    val jsed = JavaScriptOperationFinder.fromTypeScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    jsed.name should be("Simple")

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    // This should not work beause it doesn't meet the content pattern
    an[IllformedParametersException] should be thrownBy (jsed.modify(target, SimpleProjectOperationArguments("", Map("content" -> "Bjarn Stroustrup is God"))))
  }

  it should "handle default parameter values" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/SimpleEditor.ts",
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
    val jsed = JavaScriptOperationFinder.fromTypeScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
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
    val as = SimpleFileBasedArtifactSource(tsf)
    val jsed = JavaScriptOperationFinder.fromTypeScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
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
    val jsed = JavaScriptOperationFinder.fromTypeScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
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

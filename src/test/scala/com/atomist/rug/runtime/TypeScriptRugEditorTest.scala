package com.atomist.rug.runtime

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.common.IllformedParametersException
import com.atomist.project.edit.SuccessfulModification
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object TypeScriptRugEditorTest {

  val ContentPattern = "Anders .*"

  val SimpleEditor =
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
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        return `Edited Project now containing ${project.fileCount()} files: \n`;
      |    }
      |}
    """.stripMargin

  val SimpleGenerator =
    """
      |import {ProjectEditor} from 'user-model/operations/ProjectEditor'
      |import {GeneratorParameters} from 'user-model/operations/ProjectGenerator'
      |import {ProjectGenerator} from 'user-model/operations/ProjectGenerator'
      |import {Project} from 'user-model/model/Core'
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
      |        return `Edited Project now containing ${project.fileCount()} files: \n`;
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
       |      project.addFile("src/from/typescript", p.content);
       |      return `Edited Project now containing $${project.fileCount()} files: \n`;
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
      |      var t: string = `param=${ji.packageName},filecount=${m.root().fileCount()}`
      |      for (let n of m.matches())
      |        t += `Matched file=${n.path()}`;
      |
      |        var s: string = ""
      |
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        for (let f of project.files())
      |            s = s + `File [${f.path()}] containing [${f.content()}]\n`
      |        return `${t}\n\nEdited Project containing ${project.fileCount()} files: \n${s}`;
      |    }
      |  }
      | """.stripMargin

}

class TypeScriptRugEditorTest extends FlatSpec with Matchers {

  import TypeScriptRugEditorTest._

  it should "run simple editor compiled from TypeScript" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/SimpleEditor.ts", SimpleEditor))
  }

  it should "find tags" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/SimpleEditor.ts", SimpleEditorTaggedAndMeta))
    ed.tags.size should be(2)
    ed.tags.map(_.name).toSet should equal(Set("java", "maven"))
  }

  it should "find parameter metadata" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/SimpleEditor.ts", SimpleEditorTaggedAndMeta))
    ed.parameters.size should be(1)
    val p = ed.parameters.head
    p.name should be("content")
    p.description should be("Content")
    p.getDisplayName should be("content")
    p.getPattern should be(ContentPattern)
    p.getMaxLength should be(100)
  }

  it should "find description" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/SimpleEditor.ts", SimpleEditorTaggedAndMeta))
    ed.description should be("A nice little editor")
  }

  it should "have the PathExpressionEngine injected" in {
    val ed = invokeAndVerifyConstructed(StringFileArtifact(s".atomist/ConstructedEditor.ts",
      EditorInjectedWithPathExpression))
    //ed.description should be ("A nice little editor")
  }

  it should "send editor bad input and get appropriate response" in {
    val as = SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/Simple.ts", SimpleEditorTaggedAndMeta))
    val jsed = JavaScriptOperationFinder.fromTypeScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    jsed.name should be("Simple")

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    // This should not work beause it doesn't meet the content pattern
    an[IllformedParametersException] should be thrownBy (jsed.modify(target, SimpleProjectOperationArguments("", Map("content" -> "Bjarn Stroustrup is God")))
      )
    //    match {
    //      case fm: FailedModificationAttempt =>
    //    }
  }

  private def invokeAndVerifyConstructed(tsf: FileArtifact): JavaScriptInvokingProjectEditor = {
    val as = SimpleFileBasedArtifactSource(tsf)
    val jsed = JavaScriptOperationFinder.fromTypeScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    jsed.name should be("Constructed")

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    jsed.modify(target, SimpleProjectOperationArguments("", Map("packageName" -> "com.atomist.crushed"))) match {
      case sm: SuccessfulModification =>
      //sm.comment.contains("OK") should be(true)
    }
    jsed
  }

  private def invokeAndVerifySimple(tsf: FileArtifact): JavaScriptInvokingProjectEditor = {
    val as = SimpleFileBasedArtifactSource(tsf)
    val jsed = JavaScriptOperationFinder.fromTypeScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    jsed.name should be("Simple")

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    jsed.modify(target, SimpleProjectOperationArguments("", Map("content" -> "Anders Hjelsberg is God"))) match {
      case sm: SuccessfulModification =>
        sm.result.totalFileCount should be(2)
        sm.result.findFile("src/from/typescript").get.content.contains("Anders") should be(true)
    }
    jsed
  }
}

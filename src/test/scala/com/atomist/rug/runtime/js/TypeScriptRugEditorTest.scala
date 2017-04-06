package com.atomist.rug.runtime.js

import com.atomist.param.{ParameterValues, SimpleParameterValues, Tag}
import com.atomist.project.ProjectOperation
import com.atomist.project.common.IllformedParametersException
import com.atomist.project.edit._
import com.atomist.rug.{RugArchiveReader, SimpleRugResolver}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ListBuffer

object TypeScriptRugEditorTest {

  val ContentPattern = "^Anders .*$"

  val compiler = TypeScriptBuilder.compiler

  val SimpleEditorWithoutParameters =
    """
      |import {Project} from '@atomist/rug/model/Core';
      |import {Editor} from '@atomist/rug/operations/Decorators'
      |import {EditProject} from '@atomist/rug/operations/ProjectEditor'
      |
      |@Editor("Simple", "My simple editor")
      |class SimpleEditor implements EditProject {
      |    edit(project: Project): void {
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |    }
      |}
      |export let myeditor = new SimpleEditor();
    """.stripMargin

  val SimpleEditorWithBasicParameter =
    s"""
      |import {Project} from '@atomist/rug/model/Core'
      |import {Parameter, Editor} from '@atomist/rug/operations/Decorators'
      |
      |@Editor("Simple", "My simple editor")
      |class SimpleEditor {
      |
      |    @Parameter({description: "Content", pattern: "$ContentPattern"})
      |    content: string
      |
      |    edit(project: Project)  {
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God")
      |    }
      |}
      |export let myeditor = new SimpleEditor()
    """.stripMargin

  val SimpleEditorWithBasicNameParameter =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {Parameter, Editor} from '@atomist/rug/operations/Decorators'
       |
       |@Editor("Simple", "My simple editor")
       |class SimpleEditor {
       |
       |    @Parameter({description: "Name", pattern: "^.*$$"})
       |    name: string = "Not reserved"
       |
       |    @Parameter({description: "Description", pattern: "^.*$$"})
       |    description: string = "Not reserved"
       |
       |    edit(project: Project)  {
       |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
       |        if(this.name !== "Not reserved") throw new Error("Darn - name is reserved it seems");
       |        if(this.description !== "Not reserved") throw new Error("Darn - description is reserved it seems");
       |    }
       |}
       |export let myeditor = new SimpleEditor()
    """.stripMargin

  val SimpleLetStyleEditorWithoutParameters =
    """
      |import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |
      |export let editor: ProjectEditor  = {
      |    name: "Simple",
      |    description: "My simple editor",
      |    edit(project: Project) {
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |    }
      |}
    """.stripMargin

  val SimpleEditor =
    """
      |import {Project} from '@atomist/rug/model/Core'
      |import {Editor} from '@atomist/rug/operations/Decorators'
      |
      |@Editor("Simple", "My simple editor")
      |class SimpleEditor {
      |    edit(project: Project) {
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |    }
      |}
      |
      |export let myeditor = new SimpleEditor()
    """.stripMargin

  val OtherEditor =
    """
      |import {Project} from '@atomist/rug/model/Core'
      |import {Editor} from '@atomist/rug/operations/Decorators'
      |
      |@Editor("other", "My simple editor")
      |class SimpleEditor {
      |    edit(project: Project) {
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |    }
      |}
      |
      |export let myeditor = new SimpleEditor()
    """.stripMargin

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

  val SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {File} from '@atomist/rug/model/Core'
       |import {Parameter, Editor, Tags} from '@atomist/rug/operations/Decorators'
       |
       |@Editor("Simple", "My simple editor")
       |@Tags("java", "maven")
       |class SimpleEditor {
       |
       |    @Parameter({description: "Content", displayName: "content", pattern: "$ContentPattern", maxLength: 100, tags: ["foo","bar"]})
       |    content: string
       |
       |    edit(project: Project) {
       |      project.editWith("other", { otherParam: "Anders Hjelsberg is God" })
       |    }
       |  }
       |export let myeditor = new SimpleEditor()
    """.stripMargin

  val SimpleGenerator =
    """
      |import {Project} from '@atomist/rug/model/Core'
      |import {Generator} from '@atomist/rug/operations/Decorators'
      |
      |@Generator("SimpleGenerator","My simple Generator")
      |class SimpleGenerator{
      |
      |     content: string = "woot"
      |
      |     populate(project: Project) {
      |        let len: number = this.content.length;
      |        if(project.name != "woot"){
      |           throw Error(`Project name should be woot, but was ${project.name}`)
      |        }
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |    }
      |}
      |export let gen = new SimpleGenerator()
    """.stripMargin

  val SimpleEditorTaggedAndMeta =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {Parameter, Tags, Editor} from '@atomist/rug/operations/Decorators'
       |
       |@Editor("Simple","A nice little editor")
       |@Tags("java", "maven")
       |class SimpleEditor {
       |
       |    @Parameter({description: "Content", displayName: "content", pattern: "$ContentPattern", maxLength: 100, displayable: false})
       |    content: string = "Anders is ?"
       |
       |    @Parameter({description: "some num", displayName: "num", pattern: "^\\\\d+$$", maxLength: 100})
       |    num: number = 10
       |
       |    edit(project: Project) {
       |      project.addFile("src/from/typescript", this.content)
       |    }
       |  }
       |export let myeditor = new SimpleEditor()
    """.stripMargin

    val SimpleTsUtil =
      """
        |export class Bar {
        |   doWork() : void {
        |      let num: number = 100;
        |      num += 1;
        |      //etc.
        |   }
        |}
      """.stripMargin

  val SimpleEditorWithRelativeDependency =
    """
      |import {Editor} from '@atomist/rug/operations/Decorators'
      |import {Project} from '@atomist/rug/model/Core'
      |
      |import {Bar} from './Foo'
      |
      |@Editor("Simple","My simple editor")
      |class SimpleEditor {
      |
      |    edit(project: Project) {
      |        let bar: Bar = new Bar();
      |        bar.doWork()
      |        project.describeChange(`Edited Project now containing ${project.fileCount} files: \n`)
      |    }
      |}
      |
      |export let myeditor = new SimpleEditor()
    """.stripMargin

    val EditorInjectedWithPathExpressionObject: String =
      """import {Project,File} from '@atomist/rug/model/Core'
        |import {Match, PathExpression, PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
        |import {Parameter, Editor, Tags} from '@atomist/rug/operations/Decorators'
        |
        |class PomFile extends PathExpression<Project,File> {
        |
        |    constructor() { super(`/File()[@name='pom.xml']`) }
        |}
        |
        |@Tags("java", "maven")
        |@Editor("Constructed", "A nice little editor")
        |class ConstructedEditor {
        |
        |    @Parameter({description: "The Java package name", displayName: "Java Package", pattern: "^.*$", maxLength: 100})
        |    packageName: string
        |
        |    edit(project: Project) {
        |
        |      let eng: PathExpressionEngine = project.context.pathExpressionEngine;
        |      let m = eng.evaluate<Project,File>(project, new PomFile())
        |
        |      let t: string = `param=${this.packageName},filecount=${m.root().fileCount}`
        |      for (let n of m.matches()) {
        |        t += `Matched file=${n.path}`;
        |        n.append("randomness")
        |        }
        |
        |        let s: string = ""
        |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
        |        for (let f of project.files)
        |            s = s + `File [${f.path}] containing [${f.content}]\n`
        |        project.describeChange(
        |        `${t}\n\nEdited Project containing ${project.fileCount} files: \n${s}`)
        |    }
        |  }
        |  export let myeditor = new ConstructedEditor()
        | """.stripMargin

  val EditorInjectedWithPathExpression: String =
    """import {Project, File} from '@atomist/rug/model/Core'
      |import {Match, PathExpression, PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
      |import {Editor, Parameter, Tags} from '@atomist/rug/operations/Decorators'
      |
      |@Tags("java", "maven")
      |@Editor("Constructed", "A nice little editor")
      |class ConstructedEditor {
      |
      |    @Parameter({description: "The Java package name", displayName: "Java Package", pattern: "^.*$", maxLength: 100})
      |    packageName: string
      |
      |    edit(project: Project) {
      |
      |      let eng: PathExpressionEngine = project.context.pathExpressionEngine;
      |      let pe = new PathExpression<Project,File>(`/File()[@name='pom.xml']`)
      |      let m: Match<Project,File> = eng.evaluate(project, pe)
      |
      |      var t: string = `param=${this.packageName},filecount=${m.root().fileCount}`
      |      for (let n of m.matches()) {
      |        t += `Matched file=${n.path}`;
      |        n.append("randomness")
      |        }
      |
      |        var s: string = ""
      |
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        for (let f of project.files)
      |            s = s + `File [${f.path}] containing [${f.content}]\n`
      |    }
      |  }
      |  export let myeditor = new ConstructedEditor()
      | """.stripMargin

  val EditorInjectedWithPathExpressionUsingWith: String =
    """import {Project} from '@atomist/rug/model/Core'
      |import {PathExpression} from '@atomist/rug/tree/PathExpression'
      |import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
      |import {Match} from '@atomist/rug/tree/PathExpression'
      |import {File} from '@atomist/rug/model/Core'
      |import {Editor, Parameter, Tags} from '@atomist/rug/operations/Decorators'
      |
      |
      |@Tags("java", "maven")
      |@Editor("Constructed", "A nice little editor")
      |class ConstructedEditor {
      |
      |    @Parameter({description: "The Java package name", displayName: "Java Package", pattern: "^.*$", maxLength: 100})
      |    packageName: string
      |
      |    edit(project: Project) {
      |      let eng: PathExpressionEngine = project.context.pathExpressionEngine;
      |      project.files.filter(t => false)
      |      var t: string = `param=${this.packageName},filecount=${project.fileCount}`
      |
      |      eng.with<File>(project, "/*[@name='pom.xml']", n => {
      |        t += `Matched file=${n.path}`;
      |        n.append("randomness")
      |      })
      |
      |        var s: string = ""
      |
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        for (let f of project.files)
      |            s = s + `File [${f.path}] containing [${f.content}]\n`
      |
      |        //`${t}\n\nEdited Project containing ${project.fileCount} files: \n${s}`)
      |    }
      |  }
      |
      |  export let myeditor = new ConstructedEditor()
      | """.stripMargin

  val EditorInjectedWithPathExpressionUsingWithTypeJump: String =

    """import {Match, PathExpression, PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
      |import {Project, File} from '@atomist/rug/model/Core'
      |import {Editor, Parameter, Tags} from '@atomist/rug/operations/Decorators'
      |
      |
      |@Tags("java", "maven")
      |@Editor("Constructed", "A nice little editor")
      |class ConstructedEditor {
      |
      |    @Parameter({description: "The Java package name", displayName: "Java Package", pattern: "^.*$", maxLength: 100})
      |    packageName: string
      |
      |    edit(project: Project) {
      |
      |      let eng: PathExpressionEngine = project.context.pathExpressionEngine;
      |
      |      let t: string = `param=${this.packageName},filecount=${project.fileCount}`
      |
      |      eng.with<File>(project, "/File()", n => {
      |        t += `Matched file=${n.path}`;
      |        n.append("randomness")
      |      })
      |
      |        let s: string = ""
      |
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        for (let f of project.files)
      |            s = s + `File [${f.path}] containing [${f.content}]\n`
      |
      |        //`${t}\n\nEdited Project containing ${project.fileCount} files: \n${s}`)
      |    }
      |  }
      |  export let myeditor = new ConstructedEditor()
      | """.stripMargin

}

class TypeScriptRugEditorTest extends FlatSpec with Matchers {

  import TypeScriptRugEditorTest._

  it should "allow use of name/description fields if we are using annotations" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorWithBasicNameParameter))
  }

  it should "run simple editor compiled from TypeScript without parameters using support class" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorWithoutParameters))
  }

  it should "run simple editor compiled from TypeScript without parameters defined using the let style" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleLetStyleEditorWithoutParameters))
  }

  it should "invoke a generator and pass in parameters" in {
    invokeAndVerifySimpleGenerator(StringFileArtifact(s".atomist/editors/SimpleGenerator.ts", SimpleGenerator))
  }

  it should "run simple editor twice and see no change the second time" in {
    invokeAndVerifyIdempotentSimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorWithoutParameters))
  }

  it should "run simple editor compiled from TypeScript" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditor))
  }

  it should "run simple editor compiled from TypeScript that invokes another editor with separate parameters object" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorInvokingOtherEditor))
  }

  it should "run simple editor compiled from TypeScript that invokes another editor adding to our parameters object" in {
    val jsed = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters))
    val p = jsed.parameters.head
    assert(p.getTags === ListBuffer(Tag("foo","foo"),Tag("bar","bar")))
    assert(p.isDisplayable === true)
  }

  it should "find tags" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      SimpleEditorTaggedAndMeta))
    assert(ed.tags.size === 2)
    assert(ed.tags.map(_.name).toSet === Set("java", "maven"))
  }

  it should "find a dependency in the same artifact source after compilation" in {
    val simple = StringFileArtifact(s".atomist/editors/SimpleEditorWithRelativeDep.ts",
      SimpleEditorWithRelativeDependency)
    val dep = StringFileArtifact(s".atomist/editors/Foo.ts", SimpleTsUtil)
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simple, dep))
    val jsed = RugArchiveReader(as).editors.head
  }

  it should "find parameter metadata" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      SimpleEditorTaggedAndMeta))
    assert(ed.parameters.size === 2)
    val p = ed.parameters.head
    assert(p.name === "content")
    assert(p.description === "Content")
    assert(p.getDisplayName === "content")
    assert(p.getPattern === ContentPattern)
    assert(p.getMaxLength === 100)
    assert(p.getMinLength === -1)
    assert(p.isDisplayable === false)
  }

  it should "default min/max length to -1 if not set" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      SimpleEditorWithBasicParameter))
    assert(ed.parameters.size === 1)
    val p = ed.parameters.head
    assert(p.getMinLength === -1)
    assert(p.getMaxLength === -1)
  }

  it should "find description" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      SimpleEditorTaggedAndMeta))
    assert(ed.description === "A nice little editor")
  }

  it should "have the PathExpressionEngine injected" in {
    val (ed, _) = invokeAndVerifyConstructed(StringFileArtifact(s".atomist/editors/ConstructedEditor.ts",
      EditorInjectedWithPathExpression))
    assert(ed.description === "A nice little editor")
  }

  it should "have the PathExpressionEngine injected and use an object path expression" in {
    val (ed,sm) = invokeAndVerifyConstructed(StringFileArtifact(s".atomist/editors/ConstructedEditor.ts",
      EditorInjectedWithPathExpressionObject))
    assert(ed.description === "A nice little editor")
    assert(sm.changeLogEntries.size === 1)
  }

  it should "have the PathExpressionEngine injected using PathExpressionEngine.with" in {
    val (ed, _) = invokeAndVerifyConstructed(StringFileArtifact(s".atomist/editors/ConstructedEditor.ts",
      EditorInjectedWithPathExpressionUsingWith))
    assert(ed.description === "A nice little editor")
  }

  it should "have the PathExpressionEngine injected using PathExpressionEngine.with type-jump" in {
    val ed = invokeAndVerifyConstructed(StringFileArtifact(s".atomist/editors/ConstructedEditor.ts",
      EditorInjectedWithPathExpressionUsingWithTypeJump))
  }

  it should "send editor bad input and get appropriate response" in {
    val as = SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/Simple.ts", SimpleEditorTaggedAndMeta))
    val jsed = RugArchiveReader(TypeScriptBuilder.compileWithModel(as)).editors.head.asInstanceOf[JavaScriptProjectEditor]
    assert(jsed.name === "Simple")

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    // This should not work beause it doesn't meet the content pattern
    an[IllformedParametersException] should be thrownBy (jsed.modify(target, SimpleParameterValues(Map("content" -> "Bjarn Stroustrup is God"))))
  }

  it should "handle default parameter values" in {
    val ed = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      SimpleEditorTaggedAndMeta))
    assert(ed.parameters.size === 2)
    val p = ed.parameters.head
    assert(p.name === "content")
    assert(p.description === "Content")
    assert(p.getDisplayName === "content")
    assert(p.getPattern === ContentPattern)
    assert(p.getDefaultValue === "Anders is ?")
    assert(p.getMaxLength === 100)
  }

  private  def invokeAndVerifyConstructed(tsf: FileArtifact): (JavaScriptProjectEditor, SuccessfulModification) = {
    val as = SimpleFileBasedArtifactSource(tsf)
    val jsed = RugArchiveReader(TypeScriptBuilder.compileWithModel(as)).editors.head.asInstanceOf[JavaScriptProjectEditor]
    assert(jsed.name === "Constructed")

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
    jsed.modify(target, SimpleParameterValues(Map("packageName" -> "com.atomist.crushed"))) match {
      case sm: SuccessfulModification =>
      //sm.comment.contains("OK") should be(true)
        sm.result.findFile("pom.xml").get.content.contains("randomness") should be (true)
        (jsed, sm)
      case _ => ???
    }
  }

  private  def invokeAndVerifySimpleGenerator(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil): JavaScriptProjectGenerator = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))

    val jsed = RugArchiveReader(as).generators.head.asInstanceOf[JavaScriptProjectGenerator]
    assert(jsed.name === "SimpleGenerator")

//    jsed.addToArchiveContext(others)

    val prj = jsed.generate("woot", SimpleParameterValues( Map("content" -> "Anders Hjelsberg is God")))
    assert(prj.id.name === "woot")

    jsed
  }

  private  def invokeAndVerifySimple(tsf: FileArtifact): JavaScriptProjectEditor = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf) + StringFileArtifact(s".atomist/editors/OtherEditor.ts", OtherEditor))

    val jsed = RugArchiveReader(as).editors.head.asInstanceOf[JavaScriptProjectEditor]
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

  private  def invokeAndVerifyIdempotentSimple(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil): JavaScriptProjectEditor = {
    val as = SimpleFileBasedArtifactSource(tsf)
    val jsed = RugArchiveReader(TypeScriptBuilder.compileWithModel(as)).editors.head.asInstanceOf[JavaScriptProjectEditor]
    assert(jsed.name === "Simple")

//    jsed.addToArchiveContext(others)

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    val p = SimpleParameterValues(Map("content" -> "Anders Hjelsberg is God"))
    jsed.modify(target, p) match {
      case sm: SuccessfulModification =>
        assert(sm.result.totalFileCount === 2)
        sm.result.findFile("src/from/typescript").get.content.contains("Anders") should be(true)

        jsed.modify(sm.result, p) match {
          case _: NoModificationNeeded => //yay
           //yay
          case sm: SuccessfulModification =>
              fail("That should not have reported modification")
          case _ => ???
        }
      case _ => ???
    }
    jsed
  }
}

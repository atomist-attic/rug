package com.atomist.rug.runtime.js

import com.atomist.param.SimpleParameterValues
import com.atomist.project.common.MissingParametersException
import com.atomist.project.edit.ProjectEditor
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.rug.{InvalidRugParameterDefaultValue, InvalidRugParameterPatternException, RugArchiveReader}
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object JavaScriptProjectOperationTest {

  val SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {Editor, Parameter} from '@atomist/rug/operations/Decorators'
       |import {File} from '@atomist/rug/model/Core'
       |
       |@Editor("Simple","A nice little editor")
       |class SimpleEditor  {
       |    @Parameter({description: "Content", pattern: "@url", maxLength: 100})
       |    content: string = "http://t.co"
       |
       |    edit(project: Project) {
       |    }
       |  }
       |export let editor = new SimpleEditor()
    """.stripMargin

  val SimpleEditorWithDefaultParameterValue: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {Editor, Parameter} from '@atomist/rug/operations/Decorators'
       |import {File} from '@atomist/rug/model/Core'
       |
       |@Editor("Simple","A nice little editor")
       |class SimpleEditor  {
       |
       |    @Parameter({description: "Content", pattern: "@url", maxLength: 100})
       |    content: string = "http://t.co"
       |
       |    edit(project: Project) {
       |       if(this.content != "http://t.co"){
       |          throw new Error("Content was not as expected");
       |       }
       |    }
       |  }
       |export let editor = new SimpleEditor()
    """.stripMargin

  val SimpleEditorWithRequiredParameterButNoDefault: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {Editor, Parameter} from '@atomist/rug/operations/Decorators'
       |import {File} from '@atomist/rug/model/Core'
       |
       |@Editor("Simple","A nice little editor")
       |class SimpleEditor  {
       |
            @Parameter({description: "Content", pattern: "@url", maxLength: 100})
       |    content: string
       |
       |    edit(project: Project) {
       |       if(this.content != "http://t.co"){
       |          throw new Error("Content was not as expected");
       |       }
       |    }
       |  }
       |export let editor = new SimpleEditor()
    """.stripMargin

  val SimpleEditorWithBrokenParameterPattern: String =
      s"""
         |import {Project} from '@atomist/rug/model/Core'
         |import {Editor, Parameter} from '@atomist/rug/operations/Decorators'
         |import {File} from '@atomist/rug/model/Core'
         |import {Result,Status} from '@atomist/rug/operations/RugOperation'
         |
         |@Editor("Simple","A nice little editor")
         |class SimpleEditor  {
         |
         |    @Parameter({description: "Content", pattern: "@blah", maxLength: 100})
         |    content: string = "http://t.co"
         |
         |    edit(project: Project) {
         |      project.describeChange(
         |        `Edited Project now containing $${project.fileCount} files: \n`
         |        );
         |    }
         |  }
         |export let editor = new SimpleEditor()
    """.stripMargin

  val SimpleEditorWithInvalidDefaultParameterValuePattern: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {Editor, Parameter} from '@atomist/rug/operations/Decorators'
       |import {File} from '@atomist/rug/model/Core'
       |
       |@Editor("Simple","A nice little editor")
       |class SimpleEditor  {
       |
       |    @Parameter({description: "Content", pattern: "@url", maxLength: 100})
       |    content: string = "not-a-url"
       |
       |    edit(project: Project) {
       |      // Do nothing
       |    }
       |  }
       |export let editor = new SimpleEditor()
    """.stripMargin

  val SimpleEditorWithValidDefaultParameterValueFromAlternation: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {Editor, Parameter} from '@atomist/rug/operations/Decorators'
       |import {File} from '@atomist/rug/model/Core'
       |import {Result,Status} from '@atomist/rug/operations/RugOperation'
       |
       |@Editor("Simple","A nice little editor")
       |class SimpleEditor  {
       |
       |    @Parameter({description: "Content", pattern: "^(?:http://a.b.c|http://g.co|ftp://f.co)$$", maxLength: 100})
       |    content: string = "http://g.co"
       |
       |    edit(project: Project) {
       |      project.describeChange(
       |        `Edited Project now containing $${project.fileCount} files: \n`
       |      )
       |    }
       |  }
       |export let editor = new SimpleEditor()
    """.stripMargin

  val SimpleEditorWithInvalidDefaultParameterValueAlternation: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {Editor, Parameter} from '@atomist/rug/operations/Decorators'
       |import {File} from '@atomist/rug/model/Core'
       |
       |@Editor("Simple","A nice little editor")
       |class SimpleEditor  {

       |    @Parameter({description: "Content", pattern: "^(?:http://a.b.c|http://g.co|ftp://f.co)$$", maxLength: 100})
       |    content: string = "http://g.com"
       |
       |    edit(project: Project) {
       |      // empty
       |    }
       |  }
       |export let editor = new SimpleEditor()
    """.stripMargin
}

class JavaScriptProjectOperationTest extends FlatSpec with Matchers {

  import JavaScriptProjectOperationTest._

  it should "Set the default value of a parameter correctly" in {
    val tsf = StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorWithDefaultParameterValue)
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = RugArchiveReader(as).editors.head
    assert(jsed.name === "Simple")
    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
    jsed.modify(target, SimpleParameterValues.Empty)
  }

  it should "run simple editor compiled from TypeScript and validate the pattern correctly" in {
    invokeAndVerifySimpleEditor(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters))
  }

  it should "run simple editor and throw an exception for the bad pattern" in {
    assertThrows[InvalidRugParameterPatternException] {
      invokeAndVerifySimpleEditor(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
        SimpleEditorWithBrokenParameterPattern))
    }
  }

  it should "run simple editor and throw an exception for default parameter value not matching pattern" in {
    assertThrows[InvalidRugParameterDefaultValue] {
      invokeAndVerifyEditorWithDefaults(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
        SimpleEditorWithInvalidDefaultParameterValuePattern))
    }
  }

  it should "run simple editor compiled from TypeScript and validate the default from allowed values correctly" in {
    invokeAndVerifyEditorWithDefaults(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorWithValidDefaultParameterValueFromAlternation))
  }

  it should "run simple editor and throw an exception for default parameter value not in list" in {
    assertThrows[InvalidRugParameterDefaultValue] {
      invokeAndVerifyEditorWithDefaults(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorWithInvalidDefaultParameterValueAlternation))
    }
  }

  it should "Should throw an exception if required parameters are not set" in {
    val tsf = StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorWithRequiredParameterButNoDefault)
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = RugArchiveReader(as).editors.head
    assert(jsed.name  == "Simple")
    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    assertThrows[MissingParametersException]{
      jsed.modify(target, SimpleParameterValues.Empty)
    }
  }

  private  def invokeAndVerifySimpleEditor(tsf: FileArtifact): ProjectEditor = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = RugArchiveReader(as).editors.head
    assert(jsed.name === "Simple")
    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
    jsed.modify(target, SimpleParameterValues( Map("content" -> "http://blah.com"))) match {
      case _ =>
    
    }
    jsed
  }

  private  def invokeAndVerifyEditorWithDefaults(tsf: FileArtifact): ProjectEditor = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = RugArchiveReader(as).editors.head
    assert(jsed.name === "Simple")
    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
    jsed.modify(target, SimpleParameterValues( Map[String,String]())) match {
      case _ =>
    
    }
    jsed
  }
}

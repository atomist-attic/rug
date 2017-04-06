package com.atomist.rug.runtime.js

import com.atomist.param.SimpleParameterValues
import com.atomist.project.common.MissingParametersException
import com.atomist.project.edit.ProjectEditor
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.rug.{InvalidRugParameterDefaultValue, InvalidRugParameterPatternException, RugArchiveReader}
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object JavaScriptProjectOperationTest {

  val SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
       |import {File} from '@atomist/rug/model/Core'
       |import {Parameter} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleEditor implements ProjectEditor {
       |    name: string = "Simple"
       |    description: string = "A nice little editor"
       |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "@url", maxLength: 100}]
       |    edit(project: Project, {content} : {content: string}) {
       |    }
       |  }
       |export let editor = new SimpleEditor()
    """.stripMargin

  val SimpleEditorWithDefaultParameterValue: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
       |import {File} from '@atomist/rug/model/Core'
       |import {Parameter} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleEditor implements ProjectEditor {
       |    name: string = "Simple"
       |    description: string = "A nice little editor"
       |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "@url", maxLength: 100, default: "http://t.co"}]
       |    edit(project: Project, {content} : {content: string}) {
       |       if(content != "http://t.co"){
       |          throw new Error("Content was not as expected");
       |       }
       |    }
       |  }
       |export let editor = new SimpleEditor()
    """.stripMargin

  val SimpleEditorWithRequiredParameterButNoDefault: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
       |import {File} from '@atomist/rug/model/Core'
       |import {Parameter} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleEditor implements ProjectEditor {
       |    name: string = "Simple"
       |    description: string = "A nice little editor"
       |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "@url", maxLength: 100}]
       |    edit(project: Project, {content} : {content: string}) {
       |       if(content != "http://t.co"){
       |          throw new Error("Content was not as expected");
       |       }
       |    }
       |  }
       |export let editor = new SimpleEditor()
    """.stripMargin

  val SimpleReviewerInvokingOtherEditorAndAddingToOurOwnParameters: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectReviewer} from '@atomist/rug/operations/ProjectReviewer'
       |import {File} from '@atomist/rug/model/Core'
       |import {ReviewResult, ReviewComment, Parameter} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleReviewer implements ProjectReviewer {
       |    name: string = "Simple"
       |    description: string = "A nice little reviewer"
       |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "@url", maxLength: 100}]
       |    review(project: Project, {content} : {content: string}) {
       |      return new ReviewResult("",
       |          <ReviewComment[]>[]
       |        );
       |    }
       |  }
       |export let reviewer = new SimpleReviewer()
    """.stripMargin

  val SimpleEditorWithBrokenParameterPattern: String =
      s"""
         |import {Project} from '@atomist/rug/model/Core'
         |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
         |import {File} from '@atomist/rug/model/Core'
         |import {Result,Status,Parameter} from '@atomist/rug/operations/RugOperation'
         |
         |class SimpleEditor implements ProjectEditor {
         |    name: string = "Simple"
         |    description: string = "A nice little editor"
         |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "@blah", maxLength: 100}]
         |
         |    edit(project: Project, {content} : {content: string}) {
         |      project.describeChange(
         |        `Edited Project now containing $${project.fileCount} files: \n`
         |        );
         |    }
         |  }
         |export let editor = new SimpleEditor()
    """.stripMargin

  val SimpleReviewerWithBrokenParameterPattern: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectReviewer} from '@atomist/rug/operations/ProjectReviewer'
       |import {File} from '@atomist/rug/model/Core'
       |import {ReviewResult, ReviewComment, Parameter} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleReviewer implements ProjectReviewer {
       |    name: string = "Simple"
       |    description: string = "A nice little reviewer"
       |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "@blah", maxLength: 100}]
       |    review(project: Project, {content} : {content: string}) {
       |      return new ReviewResult("",
       |          <ReviewComment[]>[]
       |        );
       |    }
       |  }
       |export let reviewer = new SimpleReviewer()
    """.stripMargin

  val SimpleEditorWithInvalidDefaultParameterValuePattern: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
       |import {File} from '@atomist/rug/model/Core'
       |import {Parameter} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleEditor implements ProjectEditor {
       |    name: string = "Simple"
       |    description: string = "A nice little editor"
       |    parameters: Parameter[] = [
       |      {
       |        name: "content",
       |        description: "Content",
       |        pattern: "@url",
       |        default: "not-a-url",
       |        maxLength: 100
       |      }
       |    ]
       |    edit(project: Project, {content} : {content: string}) {
       |      // Do nothing
       |    }
       |  }
       |export let editor = new SimpleEditor()
    """.stripMargin

  val SimpleEditorWithValidDefaultParameterValueFromAlternation: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
       |import {File} from '@atomist/rug/model/Core'
       |import {Result,Status,Parameter} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleEditor implements ProjectEditor {
       |    name: string = "Simple"
       |    description: string = "A nice little editor"
       |    parameters: Parameter[] = [
       |      {
       |        name: "content",
       |        description: "Content",
       |        pattern: "^(?:http://a.b.c|http://g.co|ftp://f.co)$$",
       |        default: "http://g.co",
       |        maxLength: 100
       |      }
       |    ]
       |    edit(project: Project, {content} : {content: string}) {
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
       |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
       |import {File} from '@atomist/rug/model/Core'
       |import {Parameter} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleEditor implements ProjectEditor {
       |    name: string = "Simple"
       |    description: string = "A nice little editor"
       |    parameters: Parameter[] = [
       |      {
       |        name: "content",
       |        description: "Content",
       |        pattern: "^(?:http://a.b.c|http://g.co|ftp://f.co)$$",
       |        default: "http://g.com",
       |        maxLength: 100
       |      }
       |    ]
       |    edit(project: Project, {content} : {content: string}) {
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

  it should "run simple reviewer compiled from TypeScript and validate the pattern correctly" in {
    invokeAndVerifySimpleReviewer(StringFileArtifact(s".atomist/reviewers/SimpleReviewer.ts", SimpleReviewerInvokingOtherEditorAndAddingToOurOwnParameters))
  }

  it should "run simple reviewer and throw an exception for the bad pattern" in {
    assertThrows[InvalidRugParameterPatternException] {
      invokeAndVerifySimpleReviewer(StringFileArtifact(s".atomist/reviewers/SimpleReviewer.ts", SimpleReviewerWithBrokenParameterPattern))
    }
  }

  it should "run simple editor and throw an exception for default parameter value not matching pattern" in {
    assertThrows[InvalidRugParameterDefaultValue] {
      invokeAndVerifyEditorWithDefaults(StringFileArtifact(s".atomist/reviewers/SimpleEditor.ts",
        SimpleEditorWithInvalidDefaultParameterValuePattern))
    }
  }

  it should "run simple editor compiled from TypeScript and validate the default from allowed values correctly" in {
    invokeAndVerifyEditorWithDefaults(StringFileArtifact(s".atomist/reviewers/SimpleEditor.ts", SimpleEditorWithValidDefaultParameterValueFromAlternation))
  }

  it should "run simple editor and throw an exception for default parameter value not in list" in {
    assertThrows[InvalidRugParameterDefaultValue] {
      invokeAndVerifyEditorWithDefaults(StringFileArtifact(s".atomist/reviewers/SimpleEditor.ts", SimpleEditorWithInvalidDefaultParameterValueAlternation))
    }
  }

  it should "create two separate js objects for each operation" in {
    val tsf = StringFileArtifact(s".atomist/reviewers/SimpleEditor.ts", SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters)
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = RugArchiveReader(as).editors.head.asInstanceOf[JavaScriptProjectEditor]
    val v1 = jsed.cloneVar(jsed.jsc, jsed.jsVar)
    v1.put("name", "dude")
    jsed.jsVar.get("name") should be ("Simple")
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

  private  def invokeAndVerifySimpleReviewer(tsf: FileArtifact): ProjectReviewer = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsr = RugArchiveReader(as).reviewers.head
    assert(jsr.name === "Simple")
    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
    jsr.review(target, SimpleParameterValues( Map("content" -> "http://blah.com"))) match {
      case _ =>
    
    }
    jsr
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

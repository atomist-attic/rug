package com.atomist.rug.runtime.js

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.rug.{InvalidRugParameterDefaultValue, InvalidRugParameterPatternException, TestUtils}
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object JavaScriptInvokingProjectOperationTest {

  val SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
       |import {File} from '@atomist/rug/model/Core'
       |import {Result,Status,Parameter} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleEditor implements ProjectEditor {
       |    name: string = "Simple"
       |    description: string = "A nice little editor"
       |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "@url", maxLength: 100}]
       |    edit(project: Project, {content} : {content: string}) {
       |      return new Result(Status.Success,
       |        `Edited Project now containing $${project.fileCount()} files: \n`
       |        );
       |    }
       |  }
       |var editor = new SimpleEditor()
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
       |var reviewer = new SimpleReviewer()
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
         |      return new Result(Status.Success,
         |        `Edited Project now containing $${project.fileCount()} files: \n`
         |        );
         |    }
         |  }
         |var editor = new SimpleEditor()
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
       |var reviewer = new SimpleReviewer()
    """.stripMargin

  val SimpleEditorWithInvalidDefaultParameterValuePattern: String =
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
       |        pattern: "@url",
       |        default: "not-a-url",
       |        maxLength: 100
       |      }
       |    ]
       |    edit(project: Project, {content} : {content: string}) {
       |      return new Result(Status.Success,
       |        `Edited Project now containing $${project.fileCount()} files: \n`
       |      );
       |    }
       |  }
       |var editor = new SimpleEditor()
    """.stripMargin

  val SimpleEditorWithValidDefaultParameterValueFromList: String =
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
       |        pattern: "@url",
       |        allowed_values: [ "http://a.b.c", "http://g.co", "ftp://f.co" ],
       |        default: "http://g.co",
       |        maxLength: 100
       |      }
       |    ]
       |    edit(project: Project, {content} : {content: string}) {
       |      return new Result(Status.Success,
       |        `Edited Project now containing $${project.fileCount()} files: \n`
       |      );
       |    }
       |  }
       |var editor = new SimpleEditor()
    """.stripMargin

  val SimpleEditorWithInvalidDefaultParameterValueList: String =
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
       |        pattern: "@url",
       |        allowed_values: [ "http://a.b.c", "http://g.co", "ftp://f.co" ],
       |        default: "http://g.com",
       |        maxLength: 100
       |      }
       |    ]
       |    edit(project: Project, {content} : {content: string}) {
       |      return new Result(Status.Success,
       |        `Edited Project now containing $${project.fileCount()} files: \n`
       |      );
       |    }
       |  }
       |var editor = new SimpleEditor()
    """.stripMargin
}

class JavaScriptInvokingProjectOperationTest extends FlatSpec with Matchers {

  import JavaScriptInvokingProjectOperationTest._

  it should "run simple editor compiled from TypeScript and validate the pattern correctly" in {
    invokeAndVerifySimpleEditor(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters))
  }

  it should "run simple editor and throw an exception for the bad pattern" in {
    assertThrows[InvalidRugParameterPatternException] {
      invokeAndVerifySimpleEditor(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorWithBrokenParameterPattern))
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
      invokeAndVerifyEditorWithDefaults(StringFileArtifact(s".atomist/reviewers/SimpleEditor.ts", SimpleEditorWithInvalidDefaultParameterValuePattern))
    }
  }

  it should "run simple editor compiled from TypeScript and validate the default from allowed values correctly" in {
    invokeAndVerifyEditorWithDefaults(StringFileArtifact(s".atomist/reviewers/SimpleEditor.ts", SimpleEditorWithValidDefaultParameterValueFromList))
  }

  it should "run simple editor and throw an exception for default parameter value not in list" in {
    assertThrows[InvalidRugParameterDefaultValue] {
      invokeAndVerifyEditorWithDefaults(StringFileArtifact(s".atomist/reviewers/SimpleEditor.ts", SimpleEditorWithInvalidDefaultParameterValueList))
    }
  }

  private def invokeAndVerifySimpleEditor(tsf: FileArtifact): JavaScriptInvokingProjectEditor = {
    val as = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    jsed.name should be("Simple")
    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
    jsed.modify(target, SimpleProjectOperationArguments("", Map("content" -> "http://blah.com"))) match {
      case _ =>
    }
    jsed
  }

  private def invokeAndVerifySimpleReviewer(tsf: FileArtifact): JavaScriptInvokingProjectReviewer = {
    val as = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsr = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectReviewer]
    jsr.name should be("Simple")
    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
    jsr.review(target, SimpleProjectOperationArguments("", Map("content" -> "http://blah.com"))) match {
      case _ =>
    }
    jsr
  }

  private def invokeAndVerifyEditorWithDefaults(tsf: FileArtifact): JavaScriptInvokingProjectEditor = {
    val as = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    jsed.name should be("Simple")
    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
    jsed.modify(target, SimpleProjectOperationArguments("", Map[String,String]())) match {
      case _ =>
    }
    jsed
  }
}

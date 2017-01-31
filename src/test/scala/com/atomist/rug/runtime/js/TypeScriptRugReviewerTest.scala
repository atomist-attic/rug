package com.atomist.rug.runtime.js

import com.atomist.project.review.{ReviewResult, Severity}
import com.atomist.project.{ProjectOperation, SimpleProjectOperationArguments}
import com.atomist.rug.TestUtils
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object TypeScriptRugReviewerTest {

  val ContentPattern = "^Anders .*$"

  val compiler = TypeScriptBuilder.compiler

  val ParameterContent = "Anders Hjelsberg is God"

  val SimpleReviewerWithoutParameters =
    """
      |import {Project} from '@atomist/rug/model/Core'
      |import {ProjectReviewer} from '@atomist/rug/operations/ProjectReviewer'
      |import {ReviewResult,ReviewComment} from '@atomist/rug/operations/RugOperation'
      |
      |class SimpleReviewer implements ProjectReviewer {
      |    name: string = "Simple"
      |    description: string = "My simple editor"
      |    review(project: Project):ReviewResult {
      |        return new ReviewResult("",
      |        <ReviewComment[]>[])
      |    }
      |}
      |export let reviewer = new SimpleReviewer()
    """.stripMargin

  val SimpleReviewerWithParameters =
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
         |      return new ReviewResult(content,
         |          <ReviewComment[]>[]
         |        );
         |    }
         |  }
         |export let reviewer = new SimpleReviewer()
    """.stripMargin

  val SimpleReviewerWithParametersSinglePartialCommentReviewResult =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectReviewer} from '@atomist/rug/operations/ProjectReviewer'
       |import {File} from '@atomist/rug/model/Core'
       |import {ReviewResult, ReviewComment, Parameter, Severity} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleReviewer implements ProjectReviewer {
       |    name: string = "Simple"
       |    description: string = "A nice little reviewer"
       |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "@url", maxLength: 100}]
       |    review(project: Project, {content} : {content: string}) {
       |      return new ReviewResult(content,
       |          [new ReviewComment(content, Severity.Broken)]
       |        );
       |    }
       |  }
       |export let reviewer = new SimpleReviewer()
    """.stripMargin

  val SimpleReviewerWithParametersMultiPartialCommentsReviewResult =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectReviewer} from '@atomist/rug/operations/ProjectReviewer'
       |import {File} from '@atomist/rug/model/Core'
       |import {ReviewResult, ReviewComment, Parameter, Severity} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleReviewer implements ProjectReviewer {
       |    name: string = "Simple"
       |    description: string = "A nice little reviewer"
       |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "@url", maxLength: 100}]
       |    review(project: Project, {content} : {content: string}) {
       |      return new ReviewResult(content, [new ReviewComment(content, Severity.Broken),
       |                                        new ReviewComment("something else", Severity.Fine)]
       |        );
       |    }
       |  }
       |export let reviewer = new SimpleReviewer()
    """.stripMargin

  val SimpleReviewerWithParametersSingleCompleteCommentReviewResult =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectReviewer} from '@atomist/rug/operations/ProjectReviewer'
       |import {File} from '@atomist/rug/model/Core'
       |import {ReviewResult, ReviewComment, Parameter, Severity} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleReviewer implements ProjectReviewer {
       |    name: string = "Simple"
       |    description: string = "A nice little reviewer"
       |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "@url", maxLength: 100}]
       |    review(project: Project, {content} : {content: string}) {
       |      return new ReviewResult(content,
       |           [new ReviewComment(
       |            content,
       |            Severity.Broken,
       |            "file.txt",
       |            1,
       |            1)]
       |        );
       |    }
       |  }
       |export let reviewer = new SimpleReviewer()
    """.stripMargin
}

class TypeScriptRugReviewerTest extends FlatSpec with Matchers {
  import TypeScriptRugReviewerTest._

  it should "run simple reviewer compiled from TypeScript without parameters" in {
    val reviewResult = invokeAndVerifySimple(
      StringFileArtifact(s".atomist/reviewers/SimpleReviewer.ts",
      SimpleReviewerWithoutParameters))

    reviewResult.note should be("")
    reviewResult.comments.isEmpty should be(true)
  }

  it should "run simple reviewer compiled from TypeScript with parameters" in {
    val reviewResult = invokeAndVerifySimple(StringFileArtifact(
      s".atomist/reviewers/SimpleReviewer.ts",
      SimpleReviewerWithParameters))

    reviewResult.note should be(ParameterContent)
    reviewResult.comments.isEmpty should be(true)
  }

  it should "run simple reviewer compiled from TypeScript with parameters and populated single partial comment ReviewResult" in {
    val reviewResult = invokeAndVerifySimple(StringFileArtifact(
      s".atomist/reviewers/SimpleReviewer.ts",
      SimpleReviewerWithParametersSinglePartialCommentReviewResult))

    reviewResult.note should be(ParameterContent)
    reviewResult.comments.isEmpty should be(false)
    val singleComment = reviewResult.comments.head
    singleComment.comment should be(ParameterContent)
    singleComment.severity should be(Severity.BROKEN)
    singleComment.fileName should be(None)
    singleComment.line should be(None)
    singleComment.column should be(None)
  }

  it should "run simple reviewer compiled from TypeScript with parameters and populated multiple partial comments in the ReviewResult" in {
    val reviewResult = invokeAndVerifySimple(StringFileArtifact(
      s".atomist/reviewers/SimpleReviewer.ts",
      SimpleReviewerWithParametersMultiPartialCommentsReviewResult))

    reviewResult.note should be(ParameterContent)
    reviewResult.comments.isEmpty should be(false)
    reviewResult.comments.length should be(2)

    val firstComment = reviewResult.comments.head
    firstComment.comment should be(ParameterContent)
    firstComment.severity should be(Severity.BROKEN)
    firstComment.fileName should be(None)
    firstComment.line should be(None)
    firstComment.column should be(None)

    val secondComment = reviewResult.comments.tail.head
    secondComment.comment should be("something else")
    secondComment.severity should be(Severity.FINE)
    secondComment.fileName should be(None)
    secondComment.line should be(None)
    secondComment.column should be(None)
  }

  it should "run simple reviewer compiled from TypeScript with parameters and populated complete comment ReviewResult" in {
    val reviewResult = invokeAndVerifySimple(StringFileArtifact(
      s".atomist/reviewers/SimpleReviewer.ts",
      SimpleReviewerWithParametersSingleCompleteCommentReviewResult))

    reviewResult.note should be(ParameterContent)
    reviewResult.comments.isEmpty should be(false)
    val singleComment = reviewResult.comments.head
    singleComment.comment should be(ParameterContent)
    singleComment.severity should be(Severity.BROKEN)
    singleComment.fileName should be(Some("file.txt"))
    singleComment.line should be(Some(1))
    singleComment.column should be(Some(1))
  }

  private def invokeAndVerifySimple(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil): ReviewResult = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))

    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectReviewer]
    jsed.name should be("Simple")
    jsed.setContext(others)

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    jsed.review(target, SimpleProjectOperationArguments("", Map("content" -> ParameterContent)))
  }
}

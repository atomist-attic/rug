package com.atomist.rug.runtime.js

import com.atomist.param.SimpleParameterValues
import com.atomist.project.ProjectOperation
import com.atomist.project.archive.SimpleJavaScriptProjectOperationFinder
import com.atomist.project.review.{ReviewResult, Severity}
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

    assert(reviewResult.note === "")
    assert(reviewResult.comments.isEmpty === true)
  }

  it should "run simple reviewer compiled from TypeScript with parameters" in {
    val reviewResult = invokeAndVerifySimple(StringFileArtifact(
      s".atomist/reviewers/SimpleReviewer.ts",
      SimpleReviewerWithParameters))

    assert(reviewResult.note === ParameterContent)
    assert(reviewResult.comments.isEmpty === true)
  }

  it should "run simple reviewer compiled from TypeScript with parameters and populated single partial comment ReviewResult" in {
    val reviewResult = invokeAndVerifySimple(StringFileArtifact(
      s".atomist/reviewers/SimpleReviewer.ts",
      SimpleReviewerWithParametersSinglePartialCommentReviewResult))

    assert(reviewResult.note === ParameterContent)
    assert(reviewResult.comments.isEmpty === false)
    val singleComment = reviewResult.comments.head
    assert(singleComment.comment === ParameterContent)
    assert(singleComment.severity === Severity.BROKEN)
    assert(singleComment.fileName === None)
    assert(singleComment.line === None)
    assert(singleComment.column === None)
  }

  it should "run simple reviewer compiled from TypeScript with parameters and populated multiple partial comments in the ReviewResult" in {
    val reviewResult = invokeAndVerifySimple(StringFileArtifact(
      s".atomist/reviewers/SimpleReviewer.ts",
      SimpleReviewerWithParametersMultiPartialCommentsReviewResult))

    assert(reviewResult.note === ParameterContent)
    assert(reviewResult.comments.isEmpty === false)
    assert(reviewResult.comments.length === 2)

    val firstComment = reviewResult.comments.head
    assert(firstComment.comment === ParameterContent)
    assert(firstComment.severity === Severity.BROKEN)
    assert(firstComment.fileName === None)
    assert(firstComment.line === None)
    assert(firstComment.column === None)

    val secondComment = reviewResult.comments.tail.head
    assert(secondComment.comment === "something else")
    assert(secondComment.severity === Severity.FINE)
    assert(secondComment.fileName === None)
    assert(secondComment.line === None)
    assert(secondComment.column === None)
  }

  it should "run simple reviewer compiled from TypeScript with parameters and populated complete comment ReviewResult" in {
    val reviewResult = invokeAndVerifySimple(StringFileArtifact(
      s".atomist/reviewers/SimpleReviewer.ts",
      SimpleReviewerWithParametersSingleCompleteCommentReviewResult))

    assert(reviewResult.note === ParameterContent)
    assert(reviewResult.comments.isEmpty === false)
    val singleComment = reviewResult.comments.head
    assert(singleComment.comment === ParameterContent)
    assert(singleComment.severity === Severity.BROKEN)
    assert(singleComment.fileName === Some("file.txt"))
    assert(singleComment.line === Some(1))
    assert(singleComment.column === Some(1))
  }

  private  def invokeAndVerifySimple(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil): ReviewResult = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))

    val jsed = SimpleJavaScriptProjectOperationFinder.find(as).reviewers.head.asInstanceOf[JavaScriptProjectReviewer]
    assert(jsed.name === "Simple")
    jsed.setContext(others)

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    jsed.review(target, SimpleParameterValues( Map("content" -> ParameterContent)))
  }
}

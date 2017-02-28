package com.atomist.rug.runtime.js

import com.atomist.param.SimpleParameterValues
import com.atomist.project.ProjectOperation
import com.atomist.project.common.IllformedParametersException
import com.atomist.project.review.{ReviewResult, Severity}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.rug.{SimpleJavaScriptProjectOperationFinder, TestUtils}
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
         |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "@any", maxLength: 100}]
         |    review(project: Project, {content} : {content: string}) {
         |      return new ReviewResult(content,
         |          <ReviewComment[]>[]
         |        );
         |    }
         |  }
         |export let reviewer = new SimpleReviewer()
    """.stripMargin

  val SimpleReviewerWithInvalidParameterPattern =
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
       |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "@any", maxLength: 100}]
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
       |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "@any", maxLength: 100}]
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
       |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "@any", maxLength: 100}]
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
    val reviewResult = reviewSimple(
      StringFileArtifact(s".atomist/reviewers/SimpleReviewer.ts",
      SimpleReviewerWithoutParameters))

    assert(reviewResult.note === "")
    assert(reviewResult.comments.isEmpty === true)
  }

  it should "fail if there is an invalid parameter" in {
    assertThrows[IllformedParametersException]{
      reviewSimple(
        StringFileArtifact(s".atomist/reviewers/SimpleReviewer.ts",
          SimpleReviewerWithInvalidParameterPattern))
    }
  }
  it should "run simple reviewer compiled from TypeScript with parameters" in {
    val reviewResult = reviewSimple(StringFileArtifact(
      s".atomist/reviewers/SimpleReviewer.ts",
      SimpleReviewerWithParameters))

    assert(reviewResult.note === ParameterContent)
    assert(reviewResult.comments.isEmpty === true)
  }

  it should "run simple reviewer compiled from TypeScript with parameters and populated single partial comment ReviewResult" in {
    val reviewResult = reviewSimple(StringFileArtifact(
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
    val reviewResult = reviewSimple(StringFileArtifact(
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

  it should "run simple reviewer compiled from TypeScript with parameters and populate complete comment ReviewResult" in {
    val reviewResult = reviewSimple(StringFileArtifact(
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

  it should "find no long lines" in {
    val target = SimpleFileBasedArtifactSource(
      StringFileArtifact("x", "No long lines"),
      StringFileArtifact("y",
        """
          |May be an issue
          |if we set an insanely low threshold for line length
          |but that would be crazy
        """.stripMargin)
    )
    val rev = TestUtils.reviewerInSideFile(this, "FindLongLines.ts")
    val rr = rev.review(target, SimpleParameterValues(Map("maxLength" -> "100")))
    rr.comments shouldBe empty
  }

  it should "find two long lines" in {
    val target = SimpleFileBasedArtifactSource(
      StringFileArtifact("x", "No long lines"),
      StringFileArtifact("y",
        """
          |May be an issue
          |if we set an insanely low threshold for line length
          |but that would be crazy
        """.stripMargin)
    )
    val rev = TestUtils.reviewerInSideFile(this, "FindLongLines.ts")
    val rr = rev.review(target, SimpleParameterValues(Map("maxLength" -> "20")))
    assert(rr.comments.size === 2)
    val first = rr.comments.head
    val second = rr.comments(1)
    assert(first.fileName === Some("y"))
    assert(second.fileName === Some("y"))
    assert(first.line === Some(2))
    assert(second.line === Some(3))
  }

  private  def reviewSimple(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil): ReviewResult = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))

    val reviewer = SimpleJavaScriptProjectOperationFinder.find(as).reviewers.head.asInstanceOf[JavaScriptProjectReviewer]
    assert(reviewer.name === "Simple")
    reviewer.addToArchiveContext(others)

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    reviewer.review(target, SimpleParameterValues( Map("content" -> ParameterContent)))
  }
}

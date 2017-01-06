package com.atomist.rug.runtime.js

import com.atomist.project.review.ReviewResult
import com.atomist.project.{ProjectOperation, SimpleProjectOperationArguments}
import com.atomist.rug.TestUtils
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object TypeScriptRugReviewerTest {

  val ContentPattern = "^Anders .*$"

  val compiler = new TypeScriptCompiler()

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
      |var reviewer = new SimpleReviewer()
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
       |      //p["otherParam"] = p.content
       |      return new ReviewResult(content,
       |          <ReviewComment[]>[]
       |        );
       |    }
       |  }
       |var reviewer = new SimpleReviewer()
    """.stripMargin
}

class TypeScriptRugReviewerTest extends FlatSpec with Matchers {
  import TypeScriptRugReviewerTest._

  it should "run simple reviewer compiled from TypeScript without parameters" in {
    val reviewResult = invokeAndVerifySimple(StringFileArtifact(s".atomist/reviewers/SimpleReviewer.ts", SimpleReviewerWithoutParameters))

    reviewResult.note should be("")
    reviewResult.comments.isEmpty should be(true)
  }

  it should "run simple reviewer compiled from TypeScript with parameters" in {
    val reviewResult = invokeAndVerifySimple(StringFileArtifact(s".atomist/reviewers/SimpleReviewer.ts", SimpleReviewerWithParameters))

    reviewResult.note should be(ParameterContent)
    reviewResult.comments.isEmpty should be(true)
  }

  private def invokeAndVerifySimple(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil): ReviewResult = {
    val as = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(tsf))

    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectReviewer]
    jsed.name should be("Simple")
    jsed.setContext(others)

    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

    jsed.review(target, SimpleProjectOperationArguments("", Map("content" -> ParameterContent)))
  }
}

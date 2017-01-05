package com.atomist.rug.runtime.js

import com.atomist.project.edit.SuccessfulModification
import com.atomist.project.{ProjectOperation, SimpleProjectOperationArguments}
import com.atomist.rug.TestUtils
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object TypeScriptRugReviewerTest {

  val ContentPattern = "^Anders .*$"

  val compiler = new TypeScriptCompiler()

  val SimpleReviewerWithoutParameters =
    """
      |import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {Result,Status} from '@atomist/rug/operations/RugOperation'
      |
      |class SimpleEditor implements ProjectEditor {
      |    name: string = "Simple"
      |    description: string = "My simple editor"
      |    edit(project: Project):Result {
      |        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
      |        return new Result(Status.Success,
      |         `Edited Project now containing ${project.fileCount()} files: \n`)
      |    }
      |}
      |var editor = new SimpleEditor()
    """.stripMargin

}

class TypeScriptRugReviewerTest extends FlatSpec with Matchers {
  import TypeScriptRugReviewerTest._

  it should "run simple reviewer compiled from TypeScript without parameters using support class" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleReviewerWithoutParameters))
  }

  private def invokeAndVerifySimple(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil): JavaScriptInvokingProjectEditor = {
    val as = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(tsf))

    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
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
}

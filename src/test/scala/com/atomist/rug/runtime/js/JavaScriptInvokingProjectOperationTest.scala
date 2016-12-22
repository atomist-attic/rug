package com.atomist.rug.runtime.js

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.rug.{InvalidRugParameterPatternException, TestUtils}
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by kipz on 05/12/2016.
  */


object JavaScriptInvokingProjectOperationTest {

    val SimpleEditorWithBrokenParameterPattern =
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
           |      //p["otherParam"] = p.content
           |      return new Result(Status.Success,
           |        `Edited Project now containing $${project.fileCount()} files: \n`
           |        );
           |    }
           |  }
           |var editor = new SimpleEditor()
    """.stripMargin

    val SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters =
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
           |      //p["otherParam"] = p.content
           |      return new Result(Status.Success,
           |        `Edited Project now containing $${project.fileCount()} files: \n`
           |        );
           |    }
           |  }
           |var editor = new SimpleEditor()
    """.stripMargin

}
class JavaScriptInvokingProjectOperationTest  extends FlatSpec with Matchers {
    import JavaScriptInvokingProjectOperationTest._

    it should "run simple editor compiled from TypeScript and validate the pattern correctly" in {
        invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters))
    }

  it should "run simple editor and throw an exception for the bad pattern" in {
    assertThrows[InvalidRugParameterPatternException]{
      invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorWithBrokenParameterPattern))
    }
  }

  private def invokeAndVerifySimple(tsf: FileArtifact): JavaScriptInvokingProjectEditor = {
        val as = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(tsf))
        val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
        jsed.name should be("Simple")
        val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
        jsed.modify(target, SimpleProjectOperationArguments("", Map("content" -> "http://blah.com"))) match {
            case _ =>
        }
        jsed
    }
}

package com.atomist.rug.runtime.js

import com.atomist.project.{SimpleProjectOperationArguments}
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by kipz on 05/12/2016.
  */


object JavaScriptInvokingProjectOperationTest {
    val SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters =
        s"""
           |import {Project} from 'user-model/model/Core'
           |import {ParametersSupport} from 'user-model/operations/Parameters'
           |import {ProjectEditor} from 'user-model/operations/ProjectEditor'
           |import {Parameters} from 'user-model/operations/Parameters'
           |import {File} from 'user-model/model/Core'
           |import {Result,Status} from 'user-model/operations/Result'
           |
           |import {parameter} from 'user-model/support/Metadata'
           |import {inject} from 'user-model/support/Metadata'
           |import {parameters} from 'user-model/support/Metadata'
           |import {tag} from 'user-model/support/Metadata'
           |import {editor} from 'user-model/support/Metadata'
           |
           |abstract class ContentInfo extends ParametersSupport {
           |
           |  @parameter({description: "Content", displayName: "content", pattern: "@url", maxLength: 100})
           |  content: string = null
           |
           |}
           |
           |@editor("A nice little editor")
           |@tag("java")
           |@tag("maven")
           |class SimpleEditor implements ProjectEditor<ContentInfo> {
           |
           |    edit(project: Project, @parameters("ContentInfo") p: ContentInfo) {
           |      p["otherParam"] = p.content
           |      return new Result(Status.Success,
           |        `Edited Project now containing $${project.fileCount()} files: \n`
           |        );
           |    }
           |  }
           |
    """.stripMargin

}
class JavaScriptInvokingProjectOperationTest  extends FlatSpec with Matchers {
    import JavaScriptInvokingProjectOperationTest._

    it should "run simple editor compiled from TypeScript and validate the pattern correctly" in {
        invokeAndVerifySimple(StringFileArtifact(s".atomist/SimpleEditor.ts", SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters))
    }
    private def invokeAndVerifySimple(tsf: FileArtifact): JavaScriptInvokingProjectEditor = {
        val as = SimpleFileBasedArtifactSource(tsf)
        val jsed = JavaScriptOperationFinder.fromTypeScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
        jsed.name should be("Simple")


        val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))

        jsed.modify(target, SimpleProjectOperationArguments("", Map("content" -> "http://blah.com"))) match {
            case _ =>
        }
        jsed
    }
}

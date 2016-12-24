package com.atomist.tree.content.text.microgrammar

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.edit.SuccessfulModification
import com.atomist.project.{ProjectOperation, SimpleProjectOperationArguments}
import com.atomist.rug.{InvalidRugParameterPatternException, TestUtils}
import com.atomist.rug.runtime.js.{JavaScriptInvokingProjectEditor, JavaScriptOperationFinder}
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class TypeScriptMicrogrammarTest extends FlatSpec with Matchers {

  val ModifiesWithSimpleMicrogrammar: String =
    """import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {PathExpression,TreeNode,Microgrammar} from '@atomist/rug/tree/PathExpression'
      |import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
      |import {Match} from '@atomist/rug/tree/PathExpression'
      |import {File} from '@atomist/rug/model/Core'
      |import {Result,Status, Parameter} from '@atomist/rug/operations/RugOperation'
      |
      |class MgEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "A nice little editor"
      |
      |    edit(project: Project) {
      |
      |      let mg = new Microgrammar('modelVersion', `<modelVersion>$modelVersion:§[a-zA-Z0-9_\\.]+§</modelVersion>`)
      |      let eng: PathExpressionEngine = project.context().pathExpressionEngine()//.customize(mg)
      |
      |      var t: string = `filecount=${project.fileCount()}`
      |
      |      eng.with<TreeNode>(project, "/*[@name='pom.xml']/modelVersion()", n => {
      |        console.log(`Matched file=${n.value()}`);
      |        //n.update('Foo bar')
      |      })
      |
      |        return new Result(Status.Success, `OK`)
      |    }
      |  }
      |  var editor = new MgEditor()
      | """.stripMargin


  it should "run use microgrammar defined in TypeScript" in pendingUntilFixed {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      ModifiesWithSimpleMicrogrammar))
  }

  private def invokeAndVerifySimple(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil): JavaScriptInvokingProjectEditor = {
    val as = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    val target = ParsingTargets.NewStartSpringIoProject
    jsed.modify(target, SimpleProjectOperationArguments("", Map("content" -> "Anders Hjelsberg is God"))) match {
      case sm: SuccessfulModification =>
        sm.result.totalFileCount should be(2)
        sm.result.findFile("src/from/typescript").get.content.contains("Anders") should be(true)
    }
    jsed
  }
}

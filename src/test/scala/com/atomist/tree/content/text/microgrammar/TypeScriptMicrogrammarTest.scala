package com.atomist.tree.content.text.microgrammar

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.edit.SuccessfulModification
import com.atomist.project.{ProjectOperation, SimpleProjectOperationArguments}
import com.atomist.rug.TestUtils
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
      |import {Result,Status, Parameter} from '@atomist/rug/operations/RugOperation'
      |
      |class MgEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "Uses microgrammars"
      |
      |    edit(project: Project) {
      |      let mg = new Microgrammar('modelVersion', `<modelVersion>$modelVersion:§[a-zA-Z0-9_\\.]+§</modelVersion>`)
      |      let eng: PathExpressionEngine = project.context().pathExpressionEngine().addType(mg)
      |
      |      eng.with<TreeNode>(project, "/*[@name='pom.xml']/modelVersion()", n => {
      |        n.update('Foo bar')
      |      })
      |      return new Result(Status.Success, `OK`)
      |    }
      |  }
      |  var editor = new MgEditor()
      | """.stripMargin


  it should "run use microgrammar defined in TypeScript" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      ModifiesWithSimpleMicrogrammar))
  }

  private def invokeAndVerifySimple(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil): JavaScriptInvokingProjectEditor = {
    val as = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    val target = ParsingTargets.NewStartSpringIoProject
    jsed.modify(target, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        sm.result.findFile("pom.xml").get.content.contains("Foo bar") should be(true)
    }
    jsed
  }
}

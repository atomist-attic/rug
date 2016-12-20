package com.atomist.tree.content.text.microgrammar

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.edit.SuccessfulModification
import com.atomist.project.{ProjectOperation, SimpleProjectOperationArguments}
import com.atomist.rug.InvalidRugParameterPatternException
import com.atomist.rug.runtime.js.{JavaScriptInvokingProjectEditor, JavaScriptOperationFinder}
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

abstract class TypeScriptMicrogrammarTest extends FlatSpec with Matchers {

  //  val findFile = "/*:File[name='pom.xml']"
  //  val mg: Microgrammar = new MatcherMicrogrammar("modelVersion",
  //    mgp.parse("<modelVersion>$modelVersion:§[a-zA-Z0-9_\\.]+§</modelVersion>"))

  val ModifiesWithSimpleMicrogrammar: String =
    """import {Project} from 'user-model/model/Core'
      |import {ProjectEditor} from 'user-model/operations/ProjectEditor'
      |import {PathExpression,TreeNode} from 'user-model/tree/PathExpression'
      |import {PathExpressionEngine} from 'user-model/tree/PathExpression'
      |import {Match} from 'user-model/tree/PathExpression'
      |import {File} from 'user-model/model/Core'
      |import {Result,Status, Parameter} from 'user-model/operations/RugOperation'
      |
      |class ConstructedEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "A nice little editor"
      |    tags: string[] = ["java", "maven"]
      |
      |    edit(project: Project) {
      |
      |      let eng: PathExpressionEngine = project.context().pathExpressionEngine();
      |
      |      var t: string = `filecount=${project.fileCount()}`
      |
      |      eng.with<TreeNode>(project, "/*:File[name='pom.xml']->modelVersion", n => {
      |        t += `Matched file=${n.value()}`;
      |        //n.append("randomness")
      |      })
      |
      |        return new Result(Status.Success,
      |        `${t}\n\nEdited Project containing ${project.fileCount()} files`)
      |    }
      |  }
      |  var editor = new ConstructedEditor()
      | """.stripMargin


  it should "run use microgrammar defined in TypeScript" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      ModifiesWithSimpleMicrogrammar))
  }

  private def invokeAndVerifySimple(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil): JavaScriptInvokingProjectEditor = {
    val as = SimpleFileBasedArtifactSource(tsf)
    val jsed = JavaScriptOperationFinder.fromTypeScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    val target = ParsingTargets.NewStartSpringIoProject
    jsed.modify(target, SimpleProjectOperationArguments("", Map("content" -> "Anders Hjelsberg is God"))) match {
      case sm: SuccessfulModification =>
        sm.result.totalFileCount should be(2)
        sm.result.findFile("src/from/typescript").get.content.contains("Anders") should be(true)
    }
    jsed
  }
}

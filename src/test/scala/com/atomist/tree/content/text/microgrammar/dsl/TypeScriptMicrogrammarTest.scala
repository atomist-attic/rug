package com.atomist.tree.content.text.microgrammar.dsl

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
      |
      |class MgEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "Uses single microgrammar"
      |
      |    edit(project: Project) {
      |      let mg = new Microgrammar('modelVersion', `<modelVersion>$version:§[a-zA-Z0-9_\\.]+§</modelVersion>`)
      |      let eng: PathExpressionEngine = project.context().pathExpressionEngine().addType(mg)
      |
      |      eng.with<TreeNode>(project, "/*[@name='pom.xml']/modelVersion()/version()", n => {
      |        n.update('Foo bar')
      |      })
      |    }
      |  }
      |  var editor = new MgEditor()
      | """.stripMargin

  val ModifiesWithSimpleMicrogrammarSplitInto2: String =
    """import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {PathExpression,TreeNode,Microgrammar} from '@atomist/rug/tree/PathExpression'
      |import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
      |import {Match} from '@atomist/rug/tree/PathExpression'
      |import {Parameter} from '@atomist/rug/operations/RugOperation'
      |
      |class MgEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "Uses 2 microgrammars"
      |
      |    edit(project: Project) {
      |      let mg1 = new Microgrammar('mv1', `$mv1:§[a-zA-Z0-9_\\.]+§</modelVersion>`)
      |      let mg2 = new Microgrammar('modelVersion', `<modelVersion>$:mv1`)
      |      let eng: PathExpressionEngine = project.context().pathExpressionEngine().addType(mg1).addType(mg2)
      |
      |      eng.with<TreeNode>(project, "/*[@name='pom.xml']/modelVersion()/mv1()", n => {
      |        if (n.value() != "4.0.0") project.fail("" + n.value())
      |        n.update('Foo bar')
      |      })
      |    }
      |  }
      |  var editor = new MgEditor()
      | """.stripMargin

  val NavigatesNestedUsingPathExpression: String =
    """import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {PathExpression,TreeNode,Microgrammar} from '@atomist/rug/tree/PathExpression'
      |import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
      |import {Match} from '@atomist/rug/tree/PathExpression'
      |import {Parameter} from '@atomist/rug/operations/RugOperation'
      |
      |class MgEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "Uses single microgrammar"
      |
      |    edit(project: Project) {
      |      let mg = new Microgrammar('method', `public $type:§[A-Za-z0-9]+§`)
      |      let eng: PathExpressionEngine = project.context().pathExpressionEngine().addType(mg)
      |
      |      eng.with<TreeNode>(project, "//File()/method()/type()", n => {
      |        //console.log(`Type=${n.nodeType()},value=${n.value()}`)
      |        n.update(n.value() + "x")
      |      })
      |    }
      |  }
      |  var editor = new MgEditor()
      | """.stripMargin

  val NavigatesNestedUsingProperty: String =
    """import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {PathExpression,TreeNode,Microgrammar} from '@atomist/rug/tree/PathExpression'
      |import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
      |import {Match} from '@atomist/rug/tree/PathExpression'
      |import {Parameter} from '@atomist/rug/operations/RugOperation'
      |
      |class MgEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "Uses single microgrammar"
      |
      |    edit(project: Project) {
      |      let mg = new Microgrammar('method', `public $type:§[A-Za-z0-9]+§`)
      |      let eng: PathExpressionEngine = project.context().pathExpressionEngine().addType(mg)
      |
      |      eng.with<any>(project, "//File()/method()", n => {
      |        //console.log(`Type=${n.nodeType()},value=${n.value()}`)
      |        n.update(n.type.value() + "x")
      |      })
      |    }
      |  }
      |  var editor = new MgEditor()
      | """.stripMargin


  it should "run use microgrammar defined in TypeScript" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      ModifiesWithSimpleMicrogrammar))
  }

  it should "run use microgrammar defined in TypeScript in 2 consts" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      ModifiesWithSimpleMicrogrammarSplitInto2))
  }

  it should "navigate nested using path expression" in {
    val as = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(
      StringFileArtifact(s".atomist/editors/SimpleEditor.ts", NavigatesNestedUsingPathExpression)))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    jsed.modify(target, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        //sm.result.findFile("pom.xml").get.content.contains("Foo bar") should be(true)
    }
    jsed
  }

  it should "navigate nested using property" in pendingUntilFixed {
    val as = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(
      StringFileArtifact(s".atomist/editors/SimpleEditor.ts", NavigatesNestedUsingProperty)))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    jsed.modify(target, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
      //sm.result.findFile("pom.xml").get.content.contains("Foo bar") should be(true)
    }
    jsed
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

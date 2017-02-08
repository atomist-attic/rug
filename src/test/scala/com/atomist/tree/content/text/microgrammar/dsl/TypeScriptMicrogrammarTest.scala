package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.edit.SuccessfulModification
import com.atomist.project.{ProjectOperation, SimpleProjectOperationArguments}
import com.atomist.rug.runtime.js.{JavaScriptInvokingProjectEditor, JavaScriptOperationFinder}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class TypeScriptMicrogrammarTest extends FlatSpec with Matchers {

  val ModifiesWithSimpleMicrogrammar: String =
    """import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {PathExpression,TreeNode,TextTreeNode,Microgrammar} from '@atomist/rug/tree/PathExpression'
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
      |      eng.with<TextTreeNode>(project, "/*[@name='pom.xml']/modelVersion()/version()", n => {
      |        n.update('Foo bar')
      |      })
      |    }
      |  }
      |export let editor = new MgEditor()
      | """.stripMargin

  val ModifiesWithSimpleMicrogrammarSplitInto2: String =
    """import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {PathExpression,TreeNode,TextTreeNode,Microgrammar} from '@atomist/rug/tree/PathExpression'
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
      |      eng.with<TextTreeNode>(project, "/*[@name='pom.xml']/modelVersion()/mv1()", n => {
      |        if (n.value() != "4.0.0") project.fail("" + n.value())
      |        n.update('Foo bar')
      |      })
      |    }
      |  }
      |export let editor = new MgEditor()
      | """.stripMargin

  val RequiresFormatInfo: String =
    """import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {PathExpression,TreeNode,TextTreeNode,Microgrammar} from '@atomist/rug/tree/PathExpression'
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
      |      eng.with<TextTreeNode>(project, "/*[@name='pom.xml']/modelVersion()/mv1()", n => {
      |        if (n.value() != "4.0.0") project.fail("" + n.value())
      |
      |        let fi = n.formatInfo()
      |        if (fi == null)
      |         throw new Error("FormatInfo was null")
      |        if (fi.start().lineNumberFrom1() < 4)
      |         throw new Error(`I don't like ${fi}`)
      |
      |        n.update('Foo bar')
      |        //console.log(fi)
      |      })
      |    }
      |  }
      |export let editor = new MgEditor()
      | """.stripMargin

  val NavigatesNestedUsingPathExpression: String =
    """import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {PathExpression,TreeNode,TextTreeNode,Microgrammar} from '@atomist/rug/tree/PathExpression'
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
      |      eng.with<TextTreeNode>(project, "//File()/method()/type()", n => {
      |        //console.log(`Type=${n.nodeType()},value=${n.value()}`)
      |        n.update(n.value() + "_x")
      |      })
      |    }
      |  }
      |export let editor = new MgEditor()
      | """.stripMargin

  val ToStringOnMicrogrammarNode: String =
    """import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {PathExpression,TreeNode,TextTreeNode,Microgrammar} from '@atomist/rug/tree/PathExpression'
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
      |      eng.with<TextTreeNode>(project, "/*[@name='pom.xml']/modelVersion()/mv1()", n => {
      |        if (n.value() != "4.0.0") project.fail("" + n.value())
      |        let msg = `The node is ${n}`
      |        //console.log(msg)
      |        n.update('Foo bar')
      |      })
      |    }
      |  }
      |export let editor = new MgEditor()
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
      |        n.update(n.type().value() + "_x")
      |      })
      |    }
      |  }
      |export let editor = new MgEditor()
      | """.stripMargin

  it should "run microgrammar defined in TypeScript" in pendingUntilFixed {
    val (originalPomContent, editedPomContent) = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      ModifiesWithSimpleMicrogrammar))
    editedPomContent.contains("<modelVersion>Foo bar</modelVersion>") should be(true)
    editedPomContent should equal(originalPomContent.replace("<modelVersion>4.0.0</modelVersion>", "<modelVersion>Foo bar</modelVersion>"))
  }

  it should "use microgrammar defined in TypeScript in 2 consts" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      ModifiesWithSimpleMicrogrammarSplitInto2))
  }

  // RJ: I think this fails because of the underlying bug in returning the wrong structure
  it should "use editor requiring FormatInfo" in pendingUntilFixed {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      RequiresFormatInfo))
  }

  it should "#283 allow toString calls on node" in {
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      ToStringOnMicrogrammarNode))
  }

  it should "navigate nested using path expression" in {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(
      StringFileArtifact(s".atomist/editors/SimpleEditor.ts", NavigatesNestedUsingPathExpression)))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    jsed.modify(target, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        sm.result.allFiles.exists(f => f.content.contains("_x"))
      case _ => ???
    }
    jsed
  }

  val NavigatesNestedAndCallsNonexistentMethod: String =
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
      |        n.setBanana("this is bananas")
      |      })
      |    }
      |  }
      |export let editor = new MgEditor()
      | """.stripMargin

  it should "throw an error when calling a method that doesn't exist" in {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(
      StringFileArtifact(s".atomist/editors/SimpleEditor.ts", NavigatesNestedAndCallsNonexistentMethod)))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    try {
      jsed.modify(target, SimpleProjectOperationArguments.Empty)
      fail("There is no setBanana method, this should fail")
    } catch {
      case e: Exception =>
        withClue(e.getMessage) {
          e.getMessage.contains("setBanana") should be(true)
        }
      case _: Throwable => ???
    }
    jsed
  }


  it should "navigate nested using property" in {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(
      StringFileArtifact(s".atomist/editors/SimpleEditor.ts", NavigatesNestedUsingProperty)))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    jsed.modify(target, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        sm.result.allFiles.exists(f => f.content.contains("_x"))
      case _ => ???
    }
    jsed
  }

  private def invokeAndVerifySimple(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil) = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    val target = ParsingTargets.NewStartSpringIoProject
    val before = target.findFile("pom.xml").get.content
    jsed.modify(target, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        sm.result.findFile("pom.xml").get.content.contains("Foo bar") should be(true)
        val after = sm.result.findFile("pom.xml").get.content
        (before, after)
      case _ => ???
    }
  }
}

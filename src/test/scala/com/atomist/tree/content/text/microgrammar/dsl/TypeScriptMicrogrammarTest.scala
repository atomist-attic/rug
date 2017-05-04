package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.param.SimpleParameterValues
import com.atomist.parse.java.ParsingTargets
import com.atomist.project.ProjectOperation
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.RugArchiveReader
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
      |import {Regex} from '@atomist/rug/tree/Microgrammars'
      |
      |class MgEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "Uses single microgrammar"
      |
      |    edit(project: Project) {
      |      let mg = new Microgrammar('modelVersion', `<modelVersion>$version</modelVersion>`, { version: Regex('[a-zA-Z0-9_\\.]+') } )
      |      let eng: PathExpressionEngine = project.context.pathExpressionEngine.addType(mg)
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
      |import {Regex} from '@atomist/rug/tree/Microgrammars'
      |import {Parameter} from '@atomist/rug/operations/RugOperation'
      |
      |class MgEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "Uses 2 microgrammars"
      |
      |    edit(project: Project) {
      |      let mg2 = new Microgrammar('modelVersion', `<modelVersion>$mv1</modelVersion`,
      |                  { mv1 : Regex('[a-zA-Z0-9_\\.]+') } )
      |      let eng: PathExpressionEngine = project.context.pathExpressionEngine.addType(mg2)
      |
      |      eng.with<TextTreeNode>(project, "/*[@name='pom.xml']/modelVersion()/mv1()", n => {
      |        if (n.value() != "4.0.0") project.fail("" + n.value()) // did this ever work??
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
      |import {Regex} from '@atomist/rug/tree/Microgrammars'
      |
      |class MgEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "Uses 2 microgrammars"
      |
      |    edit(project: Project) {
      |      let mg2 = new Microgrammar('modelVersion', `<modelVersion>$mv1</modelVersion`,
      |                  { mv1 : Regex('[a-zA-Z0-9_\\.]+') } )
      |      let eng: PathExpressionEngine = project.context.pathExpressionEngine.addType(mg2)
      |
      |      eng.with<TextTreeNode>(project, "/*[@name='pom.xml']/modelVersion()/mv1()", n => {
      |        if (n.value() != "4.0.0") project.fail("" + n.value())
      |
      |        let fi = n.formatInfo
      |        if (fi == null)
      |         throw new Error("FormatInfo was null")
      |        if (fi.start.lineNumberFrom1 < 4)
      |         throw new Error(`I don't like format info value ${fi}`)
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
      |import {Regex} from '@atomist/rug/tree/Microgrammars'
      |
      |class MgEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "Uses single microgrammar"
      |
      |    edit(project: Project) {
      |      let mg = new Microgrammar('method', `public $type`, { type: Regex('[A-Za-z0-9]+') } )
      |      let eng: PathExpressionEngine = project.context.pathExpressionEngine.addType(mg)
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
      |import {Regex} from '@atomist/rug/tree/Microgrammars'
      |
      |class MgEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "Uses 2 microgrammars"
      |
      |    edit(project: Project) {
      |      let mg2 = new Microgrammar('modelVersion', `<modelVersion>$mv1</modelVersion`,
      |                  { mv1 : Regex('[a-zA-Z0-9_\\.]+') } )
      |      let eng: PathExpressionEngine = project.context.pathExpressionEngine.addType(mg2)
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
      |import {Regex} from '@atomist/rug/tree/Microgrammars'
      |
      |class MgEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "Uses single microgrammar"
      |
      |    edit(project: Project) {
      |      let mg = new Microgrammar('method', `public $type`, { type: Regex('[A-Za-z0-9]+') } )
      |      let eng: PathExpressionEngine = project.context.pathExpressionEngine.addType(mg)
      |
      |      eng.with<any>(project, "//File()/method()", n => {
      |        //console.log(`Type=${n.nodeType()},value=${n.value()}`)
      |        n.update(n.type.value() + "_x") // replace the whole method with its type and a suffix. It doesn't make sense
      |      })
      |    }
      |  }
      |export let editor = new MgEditor()
      | """.stripMargin

  it should "run microgrammar defined in TypeScript" in {
    val (originalPomContent, editedPomContent) = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      ModifiesWithSimpleMicrogrammar))
    editedPomContent.contains("<modelVersion>Foo bar</modelVersion>") should be(true)
    editedPomContent should equal(originalPomContent.replace("<modelVersion>4.0.0</modelVersion>", "<modelVersion>Foo bar</modelVersion>"))
  }

  it should "run use microgrammar defined in TypeScript in 2 consts" in { // I have a different impl in mind
    invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts",
      ModifiesWithSimpleMicrogrammarSplitInto2))
  }

  it should "use editor requiring FormatInfo" in {
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
    val jsed = RugArchiveReader(as).editors.head
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    jsed.modify(target, SimpleParameterValues.Empty) match {
      case sm: SuccessfulModification =>
        sm.result.allFiles.exists(f => f.content.contains("_x"))
      case x => fail(s"Unexpected: $x")
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
      |import {Regex} from '@atomist/rug/tree/Microgrammars'
      |
      |class MgEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "Uses single microgrammar"
      |
      |    edit(project: Project) {
      |      let mg = new Microgrammar('method', `public $type`, { type: Regex('[A-Za-z0-9]+') } )
      |      let eng: PathExpressionEngine = project.context.pathExpressionEngine.addType(mg)
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
    val jsed = RugArchiveReader(as).editors.head
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    try {
      jsed.modify(target, SimpleParameterValues.Empty)
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
    val jsed = RugArchiveReader(as).editors.head
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    jsed.modify(target, SimpleParameterValues.Empty) match {
      case sm: SuccessfulModification =>
        sm.result.allFiles.exists(f => f.content.contains("_x"))
      case x => fail(s"Unexpected: $x")
    }
    jsed
  }

  private def invokeAndVerifySimple(tsf: FileArtifact, others: Seq[ProjectOperation] = Nil) = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = RugArchiveReader(as).editors.head
    val target = ParsingTargets.NewStartSpringIoProject
    val before = target.findFile("pom.xml").get.content
    jsed.modify(target) match {
      case sm: SuccessfulModification =>
        sm.result.findFile("pom.xml").get.content.contains("Foo bar") should be(true)
        val after = sm.result.findFile("pom.xml").get.content
        (before, after)
      case wtf => fail(s"Victory eludes us: $wtf")
    }
  }
}

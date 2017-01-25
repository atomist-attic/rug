package com.atomist.rug.runtime.js.interop

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.TestUtils
import com.atomist.rug.runtime.js.{JavaScriptInvokingProjectEditor, JavaScriptOperationFinder}
import com.atomist.source.file.ClassPathArtifactSource
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, FunSuite, Matchers}

class JavaScriptBackedTypeProviderTest extends FlatSpec with Matchers {

  val InvokesTree: String =
    """import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {PathExpression,TreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
      |import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
      |import {Match} from '@atomist/rug/tree/PathExpression'
      |import {Parameter} from '@atomist/rug/operations/RugOperation'
      |
      |
      |class BananaType implements TypeProvider {
      |
      | typeName = "banana"
      |
      | find(context: TreeNode): TreeNode[] {
      |   return [ new Banana()]
      | }
      |
      |}
      |
      |class Banana implements TreeNode {
      |
      |  nodeName(): string { return "banana" }
      |
      |  nodeType(): string[] { return [ this.nodeName()] }
      |
      |  value(): string { return "yellow" }
      |
      |  update(newValue: string) {}
      |
      |  children() { return [] }
      |
      |}
      |
      |class MgEditor implements ProjectEditor {
      |    name: string = "Constructed"
      |    description: string = "Uses single microgrammar"
      |
      |    edit(project: Project) {
      |      console.log("Editing")
      |      let mg = new BananaType()
      |      let eng: PathExpressionEngine = project.context().pathExpressionEngine().addType(mg)
      |
      |      let i = 0
      |      eng.with<any>(project, "//File()/banana()", n => {
      |        //console.log("Checking color of banana")
      |        if (n.value() != "yellow")
      |         throw new Error(`Banana is not yellow but [${n.value()}]. Sad.`)
      |        i++
      |      })
      |      if (i == 0)
      |       throw new Error("No bananas tested. Sad.")
      |    }
      |  }
      |  var editor = new MgEditor()
      | """.stripMargin

  it should "invoke tree finder with one level only" in {
    val as = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(
      StringFileArtifact(s".atomist/editors/SimpleEditor.ts", InvokesTree)))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    jsed.modify(target, SimpleProjectOperationArguments.Empty) match {
      case nmn: NoModificationNeeded =>
    }
    jsed
  }

  it should "invoke tree finder with two levels" in {
    val jsed = TestUtils.editorInSideFile(this, "TwoLevel.ts")
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    jsed.modify(target, SimpleProjectOperationArguments.Empty) match {
      case nmn: NoModificationNeeded =>
    }
    jsed
  }

  it should "invoke side effecting tree finder with two levels" in {
    val jsed = TestUtils.editorInSideFile(this, "MutatingBanana.ts")
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    jsed.modify(target, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        sm.result.allFiles.exists(f => f.name.endsWith(".java") && f.content.startsWith("I am evil!"))
    }
    jsed
  }

}

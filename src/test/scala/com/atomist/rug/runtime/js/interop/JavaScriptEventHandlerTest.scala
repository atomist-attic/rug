package com.atomist.rug.runtime.js.interop

import com.atomist.event.SystemEvent
import com.atomist.plan.TreeMaterializer
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.runtime.js.JavaScriptHandlerFinder
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.{TerminalTreeNode, TreeNode}
import com.atomist.tree.pathexpression.PathExpression
import org.scalatest.{FlatSpec, Matchers}


object JavaScriptEventHandlerTest {
  val atomistConfig: AtomistConfig = DefaultAtomistConfig
  val treeMaterializer: TreeMaterializer = TestTreeMaterializer

  val reOpenCloseIssueProgram =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {EventHandler, Plan, Message} from '@atomist/rug/operations/Handlers'
       |import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
       |export let simpleHandler: EventHandler<TreeNode,TreeNode> = {
       |  name: "ClosedIssueReopener",
       |  description: "Reopens closed issues",
       |  tags: ["github", "issues"],
       |  expression: new PathExpression<TreeNode,TreeNode>("/issue"),
       |  handle(event: Match<TreeNode, TreeNode>){
       |    let issue = event.root
       |    return new Plan();
       |  }
       |}
      """.stripMargin)
}

class JavaScriptEventHandlerTest extends FlatSpec with Matchers{

  import JavaScriptEventHandlerTest._

  it should "extract and run a handler based on new style" in {

    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(JavaScriptEventHandlerTest.reOpenCloseIssueProgram))
    val handlers = JavaScriptHandlerFinder.findEventHandlers(rugArchive, new JavaScriptHandlerContext("XX", treeMaterializer))
    handlers.size should be(1)
    val handler = handlers.head
    handler.rootNodeName should be("issue")
    val plan = handler.handle(SysEvent)
  }
}

object SysEvent extends SystemEvent ("blah", "issue", 0l)

class IssueTreeNode extends TerminalTreeNode {

  override def nodeName: String = "issue"

  override def value: String = "blah"

  def state(): String = "closed"

  val number: Int = 10

  val repo: String = "rug"

  val owner: String = "atomist"

}

object TestTreeMaterializer extends TreeMaterializer {

  override def rootNodeFor(e: SystemEvent, pe: PathExpression): TreeNode = new IssueTreeNode()

  override def hydrate(teamId: String, rawRootNode: TreeNode, pe: PathExpression): TreeNode = rawRootNode
}

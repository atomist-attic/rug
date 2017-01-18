package com.atomist.rug.runtime.js.interop

import java.util

import com.atomist.event.SystemEvent
import com.atomist.event.archive.HandlerArchiveReader
import com.atomist.param.{ParameterValue, SimpleParameterValue}
import com.atomist.plan.TreeMaterializer
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.TestUtils
import com.atomist.rug.kind.service.{Action, ActionRegistry, Callback, ConsoleMessageBuilder, Rug}
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.PathExpression
import com.atomist.util.Visitor
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

object NamedJavaScriptEventHandlerTest {
  val atomistConfig: AtomistConfig = DefaultAtomistConfig
  val treeMaterializer: TreeMaterializer = TestTreeMaterializer

  val reOpenCloseIssueProgram =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {Handler, Response, Message, Event} from '@atomist/rug/operations/Handlers'
       |import {OpenIssues, Issue, ReopenIssue} from '@atomist/rug/model/Issues'
       |
       |export let simpleHandler: Handler<Issue> = {
       |  name: "ClosedIssueReopener",
       |  description: "Reopens closed issues",
       |  tags: ["github", "issues"],
       |  expression: OpenIssues,
       |  handle(event: Event<Issue>){
       |    let issue = event.child()
       |    return new Response()
       |      .addMessage(new Message(issue)
       |        .addExecutor(new ReopenIssue("Reopen")
       |          .withNumber(issue.number())
       |          .withRepo(issue.repo())
       |          .withOwner(issue.owner())))
       |  }
       |}
      """.stripMargin)
}

class NamedJavaScriptEventHandlerTest extends FlatSpec with Matchers{

  import NamedJavaScriptEventHandlerTest._


  it should "extract and run a handler based on new style" in {
    val har = new HandlerArchiveReader(treeMaterializer, atomistConfig)
    val handlers = har.handlers("XX", TestUtils.compileWithModel(new SimpleFileBasedArtifactSource("", reOpenCloseIssueProgram)), None, Nil,
      new ConsoleMessageBuilder("XX", SimpleActionRegistry))
    handlers.size should be(1)
    val handler = handlers.head
    handler.rootNodeName should be("issue")
    handler.handle(SysEvent,null)
  }
}

object SimpleActionRegistry extends ActionRegistry {

  val rug = Rug("executor", "group", "artifact", "version", "ReopenIssue")

  override def findByName(name: String): Action = Action(name, Callback(rug), new util.ArrayList[ParameterValue]())


  override def bindParameter(action: Action, name: String, value: Object) = {
    val params = new util.ArrayList[ParameterValue](action.parameters)
    params.add(SimpleParameterValue(name,value))
    Action(action.title,action.callback,params)
  }
}

object SysEvent extends SystemEvent ("blah", "issue", 0l)

class IssueTreeNode extends TreeNode {
  /**
    * Name of the node. This may vary with individual nodes: For example,
    * with files. However, node names do not always need to be unique.
    *
    * @return name of the individual node
    */
  override def nodeName: String = "issue"

  /**
    * All nodes have values: Either a terminal value or the
    * values built up from subnodes.
    */
  override def value: String = "blah"


  def state(): String = "closed"

  val number: Int = 10

  val repo: String = "rug"

  val owner: String = "atomist"

  override def accept(v: Visitor, depth: Int): Unit = ???
}

object TestTreeMaterializer extends TreeMaterializer {

  override def rootNodeFor(e: SystemEvent, pe: PathExpression): TreeNode = new IssueTreeNode()

  override def hydrate(teamId: String, rawRootNode: TreeNode, pe: PathExpression): TreeNode = rawRootNode
}



package com.atomist.rug.runtime.js.interop

import java.util

import com.atomist.event.SystemEvent
import com.atomist.event.archive.HandlerArchiveReader
import com.atomist.param.{ParameterValue, SimpleParameterValue}
import com.atomist.plan.TreeMaterializer
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.TestUtils
import com.atomist.rug.kind.service.{Action, ActionRegistry, Callback, ConsoleMessageBuilder, Rug}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.{TerminalTreeNode, TreeNode}
import com.atomist.tree.pathexpression.PathExpression
import com.atomist.util.Visitor
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

object NamedJavaScriptEventHandlerTest {
  val atomistConfig: AtomistConfig = DefaultAtomistConfig
  val treeMaterializer: TreeMaterializer = TestTreeMaterializer

  val issuesStuff = StringFileArtifact(atomistConfig.handlersRoot + "/Issues.ts",
  """import {Executor, PathExpression} from '@atomist/rug/operations/Handlers'
    |import {TreeNode} from '@atomist/rug/tree/PathExpression'
    |
    |abstract class IssueRug extends Executor {
    |  abstract name: string
    |  abstract label?: string
    |  params: {} = {}
    |  constructor(){
    |    super();
    |  }
    |  withNumber(num: number): this {
    |    this.params["number"] = num
    |    return this
    |  }
    |
    |  withOwner(owner: string) : this {
    |    this.params["owner"] = owner;
    |    return this;
    |  }
    |  withRepo(repo: string) : this {
    |    this.params["repo"] = repo;
    |    return this;
    |  }
    |}
    |
    |export class ReopenIssue extends IssueRug {
    |  name = "ReopenIssue"
    |  constructor(readonly label: string){
    |    super()
    |  }
    |}
    |
    |export class AssignIssue extends IssueRug {
    |  name = "AssignIssue"
    |  constructor(readonly label: string){
    |    super()
    |  }
    |}
    |
    |export interface Issue extends TreeNode{
    |  number(): number
    |  repo(): string
    |  owner(): string
    |}
    |
    |class OpenIssuesExpression implements PathExpression<Issue> {
    |  expression: string = "/issue[.state()='open']"
    |  kind: Issue
    |}
    |
    |class ClosedIssuesExpression implements PathExpression<Issue> {
    |  expression: "/issue[.state()='closed']"
    |  kind: Issue
    |}
    |
    |export let ClosedIssues = new ClosedIssuesExpression()
    |export let OpenIssues = new OpenIssuesExpression()
    |""".stripMargin)

  val reOpenCloseIssueProgram =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {Handler, HandlerResult, Message, Event} from '@atomist/rug/operations/Handlers'
       |import {OpenIssues, Issue, ReopenIssue} from './Issues'
       |
       |export let simpleHandler: Handler<Issue> = {
       |  name: "ClosedIssueReopener",
       |  description: "Reopens closed issues",
       |  tags: ["github", "issues"],
       |  expression: OpenIssues,
       |  handle(event: Event<Issue>){
       |    let issue = event.child()
       |    return new HandlerResult()
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
    val handlers = har.handlers("XX", TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(reOpenCloseIssueProgram,issuesStuff)), None, Nil,
      new ConsoleMessageBuilder("XX", SimpleActionRegistry))
    assert(handlers.size === 1)
    val handler = handlers.head
    assert(handler.rootNodeName === "issue")
    handler.handle(SysEvent,null)
  }
}

object SimpleActionRegistry extends ActionRegistry {

  val rug = Rug("executor", "group", "artifact", "version", "ReopenIssue")

  override def findByName(name: String): Action = Action(name, Callback(rug), new util.ArrayList[ParameterValue]())

  override def bindParameter(action: Action, name: String, value: Object): Action = {
    val params = new util.ArrayList[ParameterValue](action.parameters)
    params.add(SimpleParameterValue(name,value))
    Action(action.title,action.callback,params)
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

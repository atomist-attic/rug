package com.atomist.rug.runtime.js

import com.atomist.graph.GraphNode
import com.atomist.param.SimpleParameterValue
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.runtime.SystemEvent
import com.atomist.rug.runtime.js.interop.JavaScriptHandlerContext
import com.atomist.rug.spi.Handlers._
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.pathexpression.PathExpression
import com.atomist.tree.{TerminalTreeNode, TreeMaterializer, TreeNode}
import org.scalatest.{DiagrammedAssertions, FlatSpec, Matchers}

object JavaScriptEventHandlerTest {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig
  val treeMaterializer: TreeMaterializer = TestTreeMaterializer

  val reOpenIssueHandlerName = "ClosedIssueReopener"

  val reOpenIssueHandlerDesc = "Reopens closed issues"

  val reOpenCloseIssueProgram =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {HandleEvent, Plan, Message} from '@atomist/rug/operations/Handlers'
       |import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
       |import {EventHandler, Tags} from '@atomist/rug/operations/Decorators'
       |
       |@EventHandler("$reOpenIssueHandlerName", "$reOpenIssueHandlerDesc", new PathExpression<TreeNode,TreeNode>("/issue"))
       |@Tags("github", "issues")
       |class SimpleHandler implements HandleEvent<TreeNode,TreeNode> {
       |  handle(event: Match<TreeNode, TreeNode>){
       |    let issue = event.root
       |    let plan = new Plan()
       |
       |    const message = new Message("message1")
       |    message.channelId = "w00t";
       |    message.addAction({instruction: {
       |                kind: "command",
       |                name: {name: "n", group: "g", artifact: "a", version: "v"}}
       |    })
       |    plan.add(message);
       |
       |    const jsonMessage = new Message({ value: "message2"})
       |    plan.add(jsonMessage);
       |
       |    plan.add({ instruction: {
       |                 kind: "execute",
       |                 name: "HTTP",
       |                 parameters: {method: "GET", url: "http://youtube.com?search=kitty&safe=true", as: "JSON"}
       |               },
       |               onSuccess: {kind: "respond", name: "Kitties"},
       |               onError: {text: "No kitties for you today!"}
       |             });
       |
       |    const anEmptyPlan = new Plan()
       |    const aPlansPlan = new Plan()
       |    aPlansPlan.add(new Message("this is a plan that is in another plan"))
       |    aPlansPlan.add({ instruction: {
       |                       kind: "generate",
       |                       name: "createSomething"
       |                     },
       |                     onSuccess: anEmptyPlan
       |                   })
       |
       |    plan.add({ instruction: {
       |                 kind: "edit",
       |                 name: "modifySomething",
       |                 parameters: {message: "planception"}
       |               },
       |               onSuccess: aPlansPlan,
       |               onError: {text: "Error!"}
       |             });
       |    return plan;
       |  }
       |}
       |export let handler = new SimpleHandler();
      """.stripMargin)
}

class JavaScriptEventHandlerTest extends FlatSpec with Matchers with DiagrammedAssertions {

  import JavaScriptEventHandlerTest._

  it should "extract and run an event handler" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(JavaScriptEventHandlerTest.reOpenCloseIssueProgram))
    val finder = new JavaScriptEventHandlerFinder(new JavaScriptHandlerContext("XX", treeMaterializer))
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    handler.rootNodeName should be("issue")
    handler.tags.size should be (2)
    handler.name should be (reOpenIssueHandlerName)
    handler.description should be (reOpenIssueHandlerDesc)
    handler.pathExpression should not be null

    val actualPlan = handler.handle(SysEvent)
    val expectedPlan = Some(Plan(
      Seq(
        Message(MessageText("message1"),
          Seq(
            Presentable(
            Instruction.Command(Instruction.Detail(
              "n",
              Some(MavenCoordinate("g", "a")),
              Nil, None
            )),None)
          ),
          Some("w00t")),
        Message(JsonBody("[value=message2]"), Nil, None)
      ),
      Seq(
        Respondable(Instruction.Execute(Instruction.Detail(
          "HTTP",
          None,
          Seq(
            SimpleParameterValue("method", "GET"),
            SimpleParameterValue("url", "http://youtube.com?search=kitty&safe=true"),
            SimpleParameterValue("as", "JSON")
          ), None
        )),
          Some(Instruction.Respond(Instruction.Detail("Kitties", None, Nil, None))),
          Some(Message(MessageText("No kitties for you today!"), Nil, None))
        ),
        Respondable(Instruction.Edit(Instruction.Detail(
          "modifySomething",
          None,
          Seq(
            SimpleParameterValue("message", "planception")
          ), None
        )),
          Some(Plan(
            Seq(Message(MessageText("this is a plan that is in another plan"), Nil, None)),
            Seq(Respondable(Instruction.Generate(Instruction.Detail("createSomething", None, Nil, None)), Some(Plan(Nil, Nil)), None))
          )),
          Some(Message(MessageText("Error!"), Nil, None))
        )
      )
    ))
    assert(actualPlan == expectedPlan)
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

  override def rootNodeFor(e: SystemEvent, pe: PathExpression): GraphNode = new IssueTreeNode()

  override def hydrate(teamId: String, rawRootNode: GraphNode, pe: PathExpression): GraphNode = rawRootNode
}

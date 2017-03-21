package com.atomist.rug.runtime.js

import com.atomist.graph.GraphNode
import com.atomist.param.{SimpleParameterValue, SimpleParameterValues}
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig, RugArchiveReader}
import com.atomist.rug.runtime.SystemEvent
import com.atomist.rug.runtime.plans.{LocalInstructionRunner, LocalPlanRunner}
import com.atomist.rug.spi.Handlers._
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.file.ClassPathArtifactSource
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.marshal.EmptyLinkableContainerTreeNode
import com.atomist.tree.pathexpression.PathExpression
import com.atomist.tree.{TerminalTreeNode, TreeMaterializer}
import org.scalatest.{DiagrammedAssertions, FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

object JavaScriptEventHandlerTest {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

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
       |               onError: {body: "No kitties for you today!"}
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
       |               onError: {body: "Error!"}
       |             });
       |    return plan;
       |  }
       |}
       |export let handler = new SimpleHandler();
      """.stripMargin)

  val noPlanBuildHandler =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {HandleEvent, Plan, Message} from '@atomist/rug/operations/Handlers'
       |import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
       |import {EventHandler, Tags} from '@atomist/rug/operations/Decorators'
       |
       |@EventHandler("BuildHandler", "Handles a Build event", new PathExpression<TreeNode,TreeNode>("/build"))
       |@Tags("github", "build")
       |class SimpleHandler implements HandleEvent<TreeNode,TreeNode> {
       |  handle(event: Match<TreeNode, TreeNode>){
       |    let issue = event.root
       |    return new Plan();
       |  }
       |}
       |export let handler = new SimpleHandler();
      """.stripMargin)

  val eventHandlerWithTreeNode =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {HandleEvent, Plan, Message} from '@atomist/rug/operations/Handlers'
       |import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
       |import {EventHandler, Tags} from '@atomist/rug/operations/Decorators'
       |
       |@EventHandler("BuildHandler", "Handles a Build event", new PathExpression<TreeNode,TreeNode>("/issue"))
       |@Tags("github", "build")
       |class SimpleHandler implements HandleEvent<TreeNode,TreeNode> {
       |  handle(event: Match<TreeNode, TreeNode>): Message{
       |     return new Message("woot").withCorrelationId("dude").withNode(event.root());
       |  }
       |}
       |export let handler = new SimpleHandler();
      """.stripMargin)

  val eventHandlerWithEmptyMatches =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {HandleEvent, Plan, Message} from '@atomist/rug/operations/Handlers'
       |import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
       |import {EventHandler, Tags} from '@atomist/rug/operations/Decorators'
       |
       |@EventHandler("BuildHandler", "Handles a Build event", new PathExpression<TreeNode,TreeNode>("/nomatch"))
       |@Tags("github", "build")
       |class SimpleHandler implements HandleEvent<TreeNode,TreeNode> {
       |  handle(event: Match<TreeNode, TreeNode>): Message{
       |     return new Message("woot").withCorrelationId("dude").withNode(event.root());
       |  }
       |}
       |export let handler = new SimpleHandler();
      """.stripMargin)

}


class JavaScriptEventHandlerTest extends FlatSpec with Matchers with DiagrammedAssertions {

  import JavaScriptEventHandlerTest._

  it should "extract and run an event handler" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(JavaScriptEventHandlerTest.reOpenCloseIssueProgram))
    val finder = new JavaScriptEventHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    handler.rootNodeName should be("issue")
    handler.tags.size should be (2)
    handler.name should be (reOpenIssueHandlerName)
    handler.description should be (reOpenIssueHandlerDesc)
    handler.pathExpression should not be null

    val actualPlan = handler.handle(LocalRugContext(TestTreeMaterializer), SysEvent)
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
  it should "return the right failure if a rug is not found" in {
    val ts = ClassPathArtifactSource.toArtifactSource("com/atomist/project/archive/MyHandlers.ts")
    val moved = ts.withPathAbove(".atomist/handlers")
    val as = TypeScriptBuilder.compileWithModel(moved)
    val ops = RugArchiveReader.find(as, Nil)
    val handler = ops.commandHandlers.find(p => p.name == "LicenseAdder").get
    val plan = handler.handle(LocalRugContext(TestTreeMaterializer), SimpleParameterValues(SimpleParameterValue("license","agpl")))
    val runner = new LocalPlanRunner(null, new LocalInstructionRunner(Nil,null,null,null))
    val response = Await.result(runner.run(plan.get, None),120.seconds)
    assert(response.log.size === 1)
    response.log.foreach {
      case error: InstructionError => assert(error.error.getMessage === "Cannot find Rug Function HTTP")
      case _ =>
    }
  }
  it should "handle empty tree nodes" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(JavaScriptEventHandlerTest.noPlanBuildHandler))
    val finder = new JavaScriptEventHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    handler.rootNodeName should be("build")
    handler.pathExpression should not be null

    val actualPlan = handler.handle(LocalRugContext(EmptyTreeMaterializer), SysEvent)
    assert(actualPlan === None)
  }

  it should "allow a correlation id and treenode to be added to a message" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(JavaScriptEventHandlerTest.eventHandlerWithTreeNode))
    val finder = new JavaScriptEventHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    handler.rootNodeName should be("issue")
    handler.pathExpression should not be null
    val actualPlan = handler.handle(LocalRugContext(TestTreeMaterializer), SysEvent)
    assert(actualPlan.nonEmpty)
    assert(actualPlan.get.messages.size === 1)
    val msg = actualPlan.get.messages.head
    assert(msg.treeNode.nonEmpty)
    assert(msg.correlationId.nonEmpty)
  }

  it should "it should not invoke the actual handler if there matches are empty" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(JavaScriptEventHandlerTest.eventHandlerWithEmptyMatches))
    val finder = new JavaScriptEventHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    handler.rootNodeName should be("nomatch")
    handler.pathExpression should not be null
    val actualPlan = handler.handle(LocalRugContext(TestTreeMaterializer), SysEvent)
    assert(actualPlan.isEmpty)
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

object EmptyTreeMaterializer extends TreeMaterializer {

  override def rootNodeFor(e: SystemEvent, pe: PathExpression): GraphNode = new EmptyLinkableContainerTreeNode

  override def hydrate(teamId: String, rawRootNode: GraphNode, pe: PathExpression): GraphNode = rawRootNode
}

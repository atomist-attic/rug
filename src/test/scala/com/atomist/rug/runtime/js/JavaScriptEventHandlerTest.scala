package com.atomist.rug.runtime.js

import com.atomist.graph.GraphNode
import com.atomist.param.{ParameterValue, SimpleParameterValue, SimpleParameterValues}
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.RugArchiveReader
import com.atomist.rug.TestUtils.contentOf
import com.atomist.rug.runtime.SystemEvent
import com.atomist.rug.runtime.js.nashorn.NashornJavaScriptEngine
import com.atomist.rug.runtime.plans._
import com.atomist.rug.spi.Handlers._
import com.atomist.rug.spi.Secret
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.file.ClassPathArtifactSource
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.marshal.{EmptyContainerGraphNode, JsonBackedContainerGraphNode}
import com.atomist.tree.pathexpression.PathExpression
import com.atomist.tree.{TerminalTreeNode, TreeMaterializer}
import org.scalatest.{DiagrammedAssertions, FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

object JavaScriptEventHandlerTest {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  val simpleEventHandlerWithSecrets = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {HandleEvent, EventPlan} from '@atomist/rug/operations/Handlers'
       |import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
       |import {EventHandler, Tags, Secrets} from '@atomist/rug/operations/Decorators'
       |
       |@EventHandler("BuildHandler", "Handles a Build event", new PathExpression<TreeNode,TreeNode>("/issue"))
       |@Tags("github", "build")
       |@Secrets("atomist/user_token", "atomist/showmethemoney")
       |class SimpleHandler implements HandleEvent<TreeNode,TreeNode> {
       |  handle(event: Match<TreeNode, TreeNode>){
       |    let issue = event.root;
       |    let result = new EventPlan();
       |    result.add({instruction: {kind: "execute", name: "ExampleFunction", parameters: {thingy: "woot"}}})
       |    return result;
       |  }
       |}
       |export let handler = new SimpleHandler();
      """.stripMargin)

  val eventHandlerWithLifecycleMessage = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "EventHandlerWithLifecycleMessage.ts"))

  val reOpenCloseIssueProgram = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "ReopenIssueHandler.ts"))

  val reOpenCloseIssueProgramRunsPathExpression = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "ReopenIssueHandlerRunsPathExpression.ts"))

  val reOpenCloseIssueProgramInArray = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "ReopenIssueHandlerInArray.ts"))

  val reOpenCloseIssueProgramInArrayCreatedByFunction = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "ReopenIssueHandlerInArrayCreatedByFunction.ts"))

  val noPlanBuildHandler = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {HandleEvent, EventPlan} from '@atomist/rug/operations/Handlers'
       |import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
       |import {EventHandler, Tags} from '@atomist/rug/operations/Decorators'
       |
       |@EventHandler("BuildHandler", "Handles a Build event", new PathExpression<TreeNode,TreeNode>("/build"))
       |@Tags("github", "build")
       |class SimpleHandler implements HandleEvent<TreeNode,TreeNode> {
       |  handle(event: Match<TreeNode, TreeNode>){
       |    let issue = event.root
       |    return new EventPlan();
       |  }
       |}
       |export let handler = new SimpleHandler();
      """.stripMargin)

  val nodesWithTagBuildHandler = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {HandleEvent, EventPlan} from '@atomist/rug/operations/Handlers'
       |import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
       |import {EventHandler, Tags} from '@atomist/rug/operations/Decorators'
       |
       |@EventHandler("BuildHandler", "Handles a Build event", new PathExpression<TreeNode,TreeNode>("/Build()/on::Repo()"))
       |@Tags("github", "build")
       |class SimpleHandler implements HandleEvent<TreeNode,TreeNode> {
       |  handle(event: Match<TreeNode, TreeNode>){
       |    let issue = event.root
       |    return new EventPlan();
       |  }
       |}
       |export let handler = new SimpleHandler();
     """.stripMargin)

  val eventHandlerWithTreeNode = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {HandleEvent, EventPlan, LifecycleMessage} from '@atomist/rug/operations/Handlers'
       |import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
       |import {EventHandler, Tags} from '@atomist/rug/operations/Decorators'
       |
       |@EventHandler("BuildHandler", "Handles a Build event", new PathExpression<TreeNode,TreeNode>("/issue"))
       |@Tags("github", "build")
       |class SimpleHandler implements HandleEvent<TreeNode,TreeNode> {
       |
       |  handle(event: Match<TreeNode, TreeNode>) {
       |     return new EventPlan().add(new LifecycleMessage(event.root, "123"));
       |  }
       |}
       |export let handler = new SimpleHandler();
      """.stripMargin)

  val eventHandlerWithEmptyMatches = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {HandleEvent, EventPlan, LifecycleMessage} from '@atomist/rug/operations/Handlers'
       |import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
       |import {EventHandler, Tags} from '@atomist/rug/operations/Decorators'
       |
       |@EventHandler("Handles a Build event", new PathExpression<TreeNode,TreeNode>("/nomatch"))
       |@Tags("github", "build")
       |class SimpleHandler implements HandleEvent<TreeNode,TreeNode> {
       |  handle(event: Match<TreeNode, TreeNode>){
       |     return new EventPlan().add(new LifecycleMessage(event.root, "123"));
       |  }
       |}
       |export let handler = new SimpleHandler();
      """.stripMargin)

}

class JavaScriptEventHandlerTest extends FlatSpec with Matchers with DiagrammedAssertions {

  import JavaScriptEventHandlerTest._

  "JavaScriptEventHandler support" should "extract and run an event handler" in
    extractAndRunEventHandler(JavaScriptEventHandlerTest.reOpenCloseIssueProgram)

  it should "extract and run an event handler that runs a path expression" in
    extractAndRunEventHandler(JavaScriptEventHandlerTest.reOpenCloseIssueProgramRunsPathExpression)

  it should "extract and run an event handler in array" in
    extractAndRunEventHandler(JavaScriptEventHandlerTest.reOpenCloseIssueProgramInArray)

  it should "extract and run an event handler in array created by function" in
    extractAndRunEventHandler(JavaScriptEventHandlerTest.reOpenCloseIssueProgramInArrayCreatedByFunction)

  private def extractAndRunEventHandler(f: FileArtifact): Unit = {
    val rugArchive = TypeScriptBuilder.compileWithModel(
      SimpleFileBasedArtifactSource(f))
    val finder = new JavaScriptEventHandlerFinder()
    val handlers = finder.find(JavaScriptEngineContextFactory.create(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    handler.rootNodeName should be("issue")
    handler.tags.size should be(2)
    handler.name should be("ClosedIssueReopener")
    handler.description should be("Reopens closed issues")
    handler.pathExpression should not be null

    val expectedMessages = Seq(
      LocallyRenderedMessage("message1",
        "text/plain",
        Seq("w00t"))
    )
    val actualPlan = handler.handle(LocalRugContext(TestTreeMaterializer), SysEvent)

    actualPlan shouldBe defined
    assert(actualPlan.get.messages == expectedMessages)

    // TODO this will no longer match as Plan can now expose a native object
  }

  it should "return plan with LifecycleMessage" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(
      SimpleFileBasedArtifactSource(JavaScriptEventHandlerTest.eventHandlerWithLifecycleMessage))
    val finder = new JavaScriptEventHandlerFinder()
    val handlers = finder.find(JavaScriptEngineContextFactory.create(rugArchive))
    handlers.size should be(1)
    val actualPlan = handlers.head.handle(LocalRugContext(TestTreeMaterializer), SysEvent)
    assert(actualPlan.nonEmpty)
    assert(actualPlan.get.messages.size === 1)
    val msg = actualPlan.get.lifecycle.head
    assert(msg.node != null)
    assert(msg.actions.length == 1)
  }

  it should "return the right failure if a rug is not found" in {
    val ts = ClassPathArtifactSource.toArtifactSource("com/atomist/project/archive/MyHandlers.ts")
    val moved = ts.withPathAbove(".atomist/handlers")
    val as = TypeScriptBuilder.compileWithModel(moved)
    val ops = RugArchiveReader(as)
    val handler = ops.commandHandlers.find(p => p.name == "LicenseAdder").get
    val plan = handler.handle(LocalRugContext(TestTreeMaterializer), SimpleParameterValues(SimpleParameterValue("license", "agpl")))
    val runner = new LocalPlanRunner(null, new LocalInstructionRunner(handler, null, null, null))
    val response = runner.run(plan.get, None)
    assert(response.log.size === 1)
    response.log.foreach {
      case error: InstructionError => assert(error.error.getMessage === "Cannot find Rug Function HTTP")
      case _ =>
    }
  }

  it should "handle empty tree nodes" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(JavaScriptEventHandlerTest.noPlanBuildHandler))
    val finder = new JavaScriptEventHandlerFinder()
    val handlers = finder.find(JavaScriptEngineContextFactory.create(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    handler.rootNodeName should be("build")
    handler.pathExpression should not be null

    val actualPlan = handler.handle(LocalRugContext(EmptyTreeMaterializer), SysEvent)
    assert(actualPlan === None)
  }

  it should "allow initial element of path expression to be NodesWithTag when empty result set received" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(JavaScriptEventHandlerTest.nodesWithTagBuildHandler))
    val finder = new JavaScriptEventHandlerFinder()
    val handlers = finder.find(JavaScriptEngineContextFactory.create(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    handler.rootNodeName should be("Build")
    handler.pathExpression should not be null

    val actualPlan = handler.handle(LocalRugContext(EmptyTreeMaterializer), SysEvent)
    assert(actualPlan === None)
  }

  it should "allow a correlation id and treenode to be added to a message" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(
      SimpleFileBasedArtifactSource(JavaScriptEventHandlerTest.eventHandlerWithTreeNode))
    val finder = new JavaScriptEventHandlerFinder()
    val handlers = finder.find(JavaScriptEngineContextFactory.create(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    handler.rootNodeName should be("issue")
    handler.pathExpression should not be null
    val actualPlan = handler.handle(LocalRugContext(TestTreeMaterializer), SysEvent)
    assert(actualPlan.nonEmpty)
    assert(actualPlan.get.messages.size === 1)
    val msg = actualPlan.get.lifecycle.head
    assert(msg.node != null)
  }

  it should "resolve secrets from the secret resolver" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleEventHandlerWithSecrets))
    val finder = new JavaScriptEventHandlerFinder()
    val handlers = finder.find(JavaScriptEngineContextFactory.create(rugArchive))
    handlers.size should be(1)
    val fn = DefaultRugFunctionRegistry.find("ExampleFunction").get.asInstanceOf[ExampleRugFunction]
    val runner = new LocalPlanRunner(null, new LocalInstructionRunner(fn, null, null, new TestSecretResolver(handlers.head) {
      override def resolveSecrets(secrets: Seq[Secret]): Seq[ParameterValue] = {
        assert(secrets.size === 1)
        assert(secrets.head.name === "very")
        assert(secrets.head.path === "/secret/thingy")
        Seq(SimpleParameterValue("very", "cool"))
      }
    }))
    val actualPlan = handlers.head.handle(LocalRugContext(TestTreeMaterializer), SysEvent)
    assert(actualPlan.nonEmpty)
    runner.run(actualPlan.get, None).log.foreach {
      case i:
        InstructionResult =>
        assert(i.response.status === Status.Success)
      case i => println(i.getClass)
    }
  }

  it should "it should not invoke the actual handler if there matches are empty" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(JavaScriptEventHandlerTest.eventHandlerWithEmptyMatches))
    val finder = new JavaScriptEventHandlerFinder()
    val handlers = finder.find(JavaScriptEngineContextFactory.create(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    handler.rootNodeName should be("nomatch")
    handler.pathExpression should not be null
    val actualPlan = handler.handle(LocalRugContext(TestTreeMaterializer), SysEvent)
    assert(actualPlan.isEmpty)
  }
}

object SysEvent extends SystemEvent("blah", "issue", 0l)

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

  override def rootNodeFor(e: SystemEvent, pe: PathExpression): GraphNode = new JsonBackedContainerGraphNode(
    new EmptyContainerGraphNode(),
    "[]",
    "0.0.1")

  override def hydrate(teamId: String, rawRootNode: GraphNode, pe: PathExpression): GraphNode = rawRootNode
}

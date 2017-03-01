package com.atomist.rug.runtime.js

import com.atomist.param._
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig, JavaScriptRugArchiveReader}
import com.atomist.project.common.MissingParametersException
import com.atomist.rug.MissingSecretException
import com.atomist.rug.runtime.{AddressableRug, CommandHandler, ResponseHandler, RugSupport}
import com.atomist.rug.runtime.plans._
import com.atomist.rug.spi.Handlers._
import com.atomist.rug.spi.{Handlers, Secret}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class JavaScriptCommandHandlerTest extends FlatSpec with Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  val kitties = "ShowMeTheKitties"
  val kittyDesc = "Search Youtube for kitty videos and post results to slack"
  val simpleCommandHandler = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    """
      |import {HandleCommand, Instruction, Response, HandlerContext, Plan, Message} from '@atomist/rug/operations/Handlers'
      |import {CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
      |
      |@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
      |@Tags("kitty", "youtube", "slack")
      |@Intent("show me kitties","cats please")
      |class KittieFetcher implements HandleCommand{
      |
      |  @Parameter({description: "his dudeness", pattern: "^.*$"})
      |  name: string
      |
      |  handle(ctx: HandlerContext) : Plan {
      |    let pxe = ctx.pathExpressionEngine()
      |
      |    if(this.name != "el duderino") {
      |      throw new Error("This will not stand");
      |    }
      |    let result = new Plan()
      |    result.add({ instruction: {
      |                 kind: "execute",
      |                 name: "HTTP",
      |                 parameters: {method: "GET", url: "http://youtube.com?search=kitty&safe=true", as: "JSON"}
      |               },
      |               onSuccess: {kind: "respond", name: "Kitties"},
      |               onError: {body: "No kitties for you today!"}})
      |    return result;
      |  }
      |}
      |
      |export let command = new KittieFetcher();
      |
    """.stripMargin)

  val simpleCommandHandlerWithMappedParams = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    """
      |import {HandleCommand, Instruction, Response, HandlerContext, Plan, Message} from '@atomist/rug/operations/Handlers'
      |import {CommandHandler, Parameter, MappedParameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
      |
      |@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
      |@Tags("kitty", "youtube", "slack")
      |@Intent("show me kitties","cats please")
      |class KittieFetcher implements HandleCommand{
      |
      |  @MappedParameter("atomist/repo")
      |  name: string
      |
      |  handle(ctx: HandlerContext) : Plan {
      |
      |    if(this.name != "el duderino") {
      |      throw new Error("This will not stand");
      |    }
      |    let result = new Plan()
      |    return result;
      |  }
      |}
      |
      |export let command = new KittieFetcher();
      |
      """.stripMargin)


  val simpleCommandHandlerHandlerWithSecrets = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    """
       |import {HandleCommand, Instruction, Response, HandlerContext, Plan, Message} from '@atomist/rug/operations/Handlers'
       |import {CommandHandler, Secrets, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
       |
       |@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
       |@Tags("kitty", "youtube", "slack")
       |@Intent("show me kitties","cats please")
       |@Secrets("atomist/user_token", "atomist/showmethemoney")
       |class KittieFetcher implements HandleCommand{
       |
       |  handle(ctx: HandlerContext) : Plan {

       |    let result = new Plan()
       |    result.add({instruction: {kind: "execute", name: "ExampleFunction", parameters: {thingy: "woot"}}})
       |    return result;
       |  }
       |}
       |
       |export let command = new KittieFetcher();
       |
    """.stripMargin)

  val simpleCommandHandlerWitPresentable = StringFileArtifact(atomistConfig.
    handlersRoot + "/Handler.ts",
    """
      |import {HandleCommand, MappedParameters, Message, Instruction, Response, HandlerContext, Plan} from '@atomist/rug/operations/Handlers'
      |import {CommandHandler, Parameter, MappedParameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
      |
      |@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
      |@Tags("kitty", "youtube", "slack")
      |@Intent("show me kitties","cats please")
      |class KittieFetcher implements HandleCommand{
      |
      |  @Parameter({description: "his dudeness", pattern: "^.*$"})
      |  name: string
      |
      |  @MappedParameter(MappedParameters.REPO_OWNER)
      |  owner: string
      |
      |  handle(ctx: HandlerContext) : Plan {
      |    let pxe = ctx.pathExpressionEngine()
      |
      |    if(this.name != "el duderino") {
      |      throw new Error("This will not stand");
      |    }
      |    let result = new Plan()
      |    let message = new Message("KittieFetcher");
      |    message.addAction({ instruction: {
      |                 kind: "command",
      |                 name: "GetKitties"},
      |                 label: "Fetch'em"})
      |    result.add(message);
      |    return result;
      |  }
      |}
      |
      |export let command = new KittieFetcher();
      |
    """.stripMargin)

  val simpleCommandHandlerExecuteInstructionCallingRespondable =
    StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
      """
        |import {HandleCommand, HandleResponse, Message, Instruction, Response, HandlerContext, Plan} from '@atomist/rug/operations/Handlers'
        |import {CommandHandler, ResponseHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
        |
        |@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
        |@Tags("kitty", "youtube", "slack")
        |@Intent("show me kitties","cats please")
        |class KittieFetcher implements HandleCommand{
        |
        |  handle(ctx: HandlerContext) : Plan {
        |
        |    let result = new Plan()
        |    result.add({instruction: {kind: "execute", name: "ExampleFunction", parameters: {thingy: "woot"}},
        |                onSuccess: {name: "SimpleResponseHandler", kind: "respond"} });
        |    return result;
        |  }
        |}
        |
        |@ResponseHandler("SimpleResponseHandler", "Checks response is equal to passed in parameter")
        |class Responder implements HandleResponse<String> {
        |  handle(response: Response<string>) : Plan {
        |    return new Plan();
        |  }
        |}
        |
        |export let respond = new Responder();
        |
        |export let command = new KittieFetcher();
        |
    """.stripMargin)

  val simpleCommandHandlerReturningMessage = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    """
      |import {HandleCommand, Instruction, Response, HandlerContext, Plan, Message} from '@atomist/rug/operations/Handlers'
      |import {CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
      |
      |@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
      |class KittieFetcher implements HandleCommand{
      |
      |  handle(ctx: HandlerContext) : Message {
      |    return new Message("Up and at 'em!");
      |  }
      |}
      |
      |export let command = new KittieFetcher();
      |
    """.stripMargin)

  it should "allow us to return a message directly from a handler" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandlerReturningMessage))
    val rugs = new JavaScriptRugArchiveReader().find(rugArchive, Nil)
    val com = rugs.commandHandlers.head
    val plan = com.handle(null,SimpleParameterValues.Empty).get
    assert(plan.messages.size === 1)
  }

  it should "be able to schedule an Execution and handle its response" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandlerExecuteInstructionCallingRespondable))
    val rugs = new JavaScriptRugArchiveReader().find(rugArchive, Nil)
    val com = rugs.commandHandlers.head
    val responseHandler = rugs.responseHandlers.head

    val plan = com.handle(null,SimpleParameterValues.Empty).get
    val runner = new LocalPlanRunner(null, new LocalInstructionRunner(Seq(new TestResponseHandler(responseHandler)), null, null, new TestSecretResolver(null) {
      override def resolveSecrets(secrets: Seq[Secret]): Seq[ParameterValue] = Nil}))

    val results = Await.result(runner.run(plan, None), 10.seconds)
    PlanUtils.drawEventLogs("testplan",results.log)
    results.log.foreach {
      case i: InstructionResult =>
      case e :InstructionError => e.error.printStackTrace()
      case i => println("Ga:" + i)
    }
  }

  it should "extract and run a command handler" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandler))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))

    handlers.size should be(1)
    val handler = handlers.head
    handler.name should be(kitties)
    handler.description should be(kittyDesc)
    handler.tags.size should be(3)
    handler.intent.size should be(2)
    val plan = handler.handle(LocalRugContext(TestTreeMaterializer), SimpleParameterValues(SimpleParameterValue("name", "el duderino")))
  }

  it should "parse messages containing Presentables" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandlerWitPresentable))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    handler.name should be(kitties)
    handler.description should be(kittyDesc)
    handler.tags.size should be(3)
    handler.intent.size should be(2)
    val plan = handler.handle(LocalRugContext(TestTreeMaterializer), SimpleParameterValues(SimpleParameterValue("owner", "his dudeness"), SimpleParameterValue("name", "el duderino")))
  }

  it should "throw exceptions if required parameters are not set" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandler))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    val handler = handlers.head
    assertThrows[MissingParametersException] {
      handler.handle(LocalRugContext(TestTreeMaterializer), SimpleParameterValues.Empty)
    }
  }

  it should "set mapped properties correctly if present" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandlerWithMappedParams))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    val handler = handlers.head
    handler.handle(LocalRugContext(TestTreeMaterializer), SimpleParameterValues(SimpleParameterValue("name", "el duderino")))
  }
  it should "expose the required secrets on a CommandHandler" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandlerHandlerWithSecrets))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    assert(handlers.head.secrets.size === 2)
    assert(handlers.head.secrets.head === Secret("atomist/user_token", "atomist/user_token"))
    assert(handlers.head.secrets(1) === Secret("atomist/showmethemoney", "atomist/showmethemoney"))
  }

  it should "resolve secrets from the secret resolver" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandlerHandlerWithSecrets))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    val fn = DefaultRugFunctionRegistry.find("ExampleFunction").get.asInstanceOf[ExampleRugFunction]
    fn.clearSecrets
    fn.addSecret(Secret("very", "/secret/thingy"))
    val runner = new LocalPlanRunner(null, new LocalInstructionRunner(Nil, null, null, new TestSecretResolver(handlers.head) {
      /**
        * Resolve a bunch of secrets at once
        *
        * @param secrets set of Secrets
        * @return a mapping from secret name (could be same as path) to actual secret value
        */
      override def resolveSecrets(secrets: Seq[Secret]): Seq[ParameterValue] = {
        assert(secrets.size === 1)
        assert(secrets.head.name === "very")
        assert(secrets.head.path === "/secret/thingy")
        Seq(SimpleParameterValue("very", "cool"))
      }
    }))
    Await.result(runner.run(handlers.head.handle(null, SimpleParameterValues.Empty).get, None), 10.seconds).log.foreach {
      case i:
        InstructionResult =>
        assert(i.response.status === Status.Success)
      case i => ???
    }
  }
}

class TestResponseHandler(r: ResponseHandler) extends AddressableRug with ResponseHandler with RugSupport {

  override def artifact: String = ???

  override def group: String = ???

  override def version: String = ???

  override def handle(response: Response, params: ParameterValues): Option[Plan] = {
    None
  }

  override def name: String = r.name

  override def description: String =r.description

  override def tags: Seq[Tag] = r.tags

  /**
    * Custom keys for this template. Must be satisfied in ParameterValues passed in.
    *
    * @return a list of parameters
    */
  override def parameters: Seq[Parameter] = ???
}




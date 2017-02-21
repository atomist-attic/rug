package com.atomist.rug.runtime.js

import com.atomist.param.{SimpleParameterValue, SimpleParameterValues}
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.project.common.MissingParametersException
import com.atomist.rug.runtime.js.interop.{LocalRugContext, jsPathExpressionEngine}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.TreeMaterializer
import org.scalatest.{FlatSpec, Matchers}

class JavaScriptCommandHandlerTest extends FlatSpec with Matchers{

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  val kitties = "ShowMeTheKitties"
  val kittyDesc = "Search Youtube for kitty videos and post results to slack"
  val simpleCommandHandler =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {HandleCommand, Instruction, Response, HandlerContext, Plan, Message} from '@atomist/rug/operations/Handlers'
       |import {CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
       |
       |@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
       |@Tags("kitty", "youtube", "slack")
       |@Intent("show me kitties","cats please")
       |class KittieFetcher implements HandleCommand{
       |
       |  @Parameter({description: "his dudeness", pattern: "^.*$$"})
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
       |               onError: {text: "No kitties for you today!"}})
       |    return result;
       |  }
       |}
       |
       |export let command = new KittieFetcher();
       |
    """.stripMargin)

  val simpleCommandHandlerWitPresentable =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {HandleCommand, MappedParameters, Message, Instruction, Response, HandlerContext, Plan} from '@atomist/rug/operations/Handlers'
       |import {CommandHandler, Parameter, MappedParameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
       |
       |@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
       |@Tags("kitty", "youtube", "slack")
       |@Intent("show me kitties","cats please")
       |class KittieFetcher implements HandleCommand{
       |
       |  @Parameter({description: "his dudeness", pattern: "^.*$$"})
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

  val simpleCommandHandlerExecuteInstruction =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    """
       |import {HandleCommand, HandleResponse, Message, Instruction, Response, HandlerContext, Plan} from '@atomist/rug/operations/Handlers'
       |import {CommandHandler, ResponseHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
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
       |
       |    let result = new Plan()
       |
       |    result.add({instruction: {kind: "execute", name: "ExampleRugFunction", parameters: {thingy: "woot"}},
       |                onSuccess: {kind: "respond", name: "SimpleResponseHandler", parameters: {sent: "woot"}}});
       |    return result;
       |  }
       |}
       |
       |@ResponseHandler("SimpleResponseHandler", "Checks response is equal to passed in parameter")
       |class Responder implements HandleResponse<String> {
       |  @Parameter({description: "Thing to check", pattern: "^.*$"})
       |  thingy: string
       |
       |  handle(response: Response<String>) : Plan {
       |    if(response.body != this.thingy) {
       |       throw new Error(`Was expecting value of response body to be ${this.thingy}, was ${response.body}`)
       |     }
       |     return new Plan();
       |  }
       |}
       |
       |export let respond = new Responder();
       |
       |export let command = new KittieFetcher();
       |
    """.stripMargin)

  it should "be able to schedule an Execution and handle its response" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandlerExecuteInstruction))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
  }

  it should "extract and run a command handler" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandler))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))

    handlers.size should be(1)
    val handler = handlers.head
    handler.name should be(kitties)
    handler.description should be (kittyDesc)
    handler.tags.size should be(3)
    handler.intent.size should be(2)
    val plan = handler.handle(LocalRugContext(TestTreeMaterializer), SimpleParameterValues(SimpleParameterValue("name","el duderino")))
  }

  it should "parse messages containing Presentables" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandlerWitPresentable))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    handler.name should be(kitties)
    handler.description should be (kittyDesc)
    handler.tags.size should be(3)
    handler.intent.size should be(2)
    val plan = handler.handle(LocalRugContext(TestTreeMaterializer), SimpleParameterValues(SimpleParameterValue("owner","his dudeness"), SimpleParameterValue("name","el duderino")))
  }

  it should "throw exceptions if required parameters are not set" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandler))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    val handler = handlers.head
    assertThrows[MissingParametersException]{
      handler.handle(LocalRugContext(TestTreeMaterializer), SimpleParameterValues.Empty)
    }
  }
}

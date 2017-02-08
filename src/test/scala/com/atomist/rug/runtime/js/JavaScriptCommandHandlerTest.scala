package com.atomist.rug.runtime.js

import com.atomist.plan.TreeMaterializer
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.runtime.js.interop.JavaScriptHandlerContext
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}


class JavaScriptCommandHandlerTest extends FlatSpec with Matchers{
  val atomistConfig: AtomistConfig = DefaultAtomistConfig
  val treeMaterializer: TreeMaterializer = TestTreeMaterializer

  val kitties = "ShowMeTheKitties"
  val kittyDesc = "Search Youtube for kitty videos and post results to slack"
  val simpleCommandHandler =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {HandleCommand, Instruction, Response, CommandContext, Plan, Message} from '@atomist/rug/operations/Handlers'
       |import {CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
       |
       |@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
       |@Tags("kitty", "youtube", "slack")
       |@Intent("show me kitties","cats please")
       |class KittieFetcher implements HandleCommand{
       |  handle(command: CommandContext) : Plan {
       |    let result = new Plan()
       |    result.add({kind: "execution",
       |                name: "HTTP",
       |                parameters: {method: "GET", url: "http://youtube.com?search=kitty&safe=true", as: "JSON"},
       |                onSuccess: {kind: "respond", name: "Kitties"},
       |                onError: {text: "No kitties for you today!"}})
       |    return result;
       |  }
       |}
       |
       |export let command = new KittieFetcher();
       |
    """.stripMargin)

  it should "extract and run a response handler" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandler))
    val handlers = JavaScriptCommandHandler.extractHandlers(rugArchive, new JavaScriptHandlerContext("XX", treeMaterializer))
    handlers.size should be(1)
    val handler = handlers.head
    handler.name should be(kitties)
    handler.description should be (kittyDesc)
    handler.tags.size should be(3)
    handler.intent.size should be(2)
    //TODO run it!
  }
}

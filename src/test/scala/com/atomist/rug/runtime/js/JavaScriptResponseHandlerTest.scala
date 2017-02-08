package com.atomist.rug.runtime.js

import com.atomist.plan.TreeMaterializer
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.runtime.js.interop.JavaScriptHandlerContext
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class JavaScriptResponseHandlerTest extends FlatSpec with Matchers{
  val atomistConfig: AtomistConfig = DefaultAtomistConfig
  val treeMaterializer: TreeMaterializer = TestTreeMaterializer

  val kitties = "Kitties"
  val kittyDesc = "Prints out kitty urls"
  val simpleResponseHandler =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
      |import {Respond, Instruction, Response, CommandContext, Plan, Message} from '@atomist/rug/operations/Handlers'
      |import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
      |import {EventHandler, ResponseHandler, CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
      |import {Project} from '@atomist/rug/model/Core'
      |import {HandleResponse, HandleEvent, HandleCommand} from '@atomist/rug/operations/Handlers'
      |
      |@ResponseHandler("$kitties", "$kittyDesc")
      |class KittiesResponder implements HandleResponse<Object>{
      |  handle(response: Response<Object>) : Message {
      |    let results = response.body as any;
      |    return new Message(results.urls.join(","))
      |  }
      |}
      |
      |export let kit = new KittiesResponder();
      |
    """.stripMargin)

  it should "extract and run a response handler" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleResponseHandler))
    val handlers = JavaScriptResponseHandler.extractHandlers(rugArchive, new JavaScriptHandlerContext("XX", treeMaterializer))
    handlers.size should be(1)
    val handler = handlers.head
    handler.name should be(kitties)
    handler.description should be (kittyDesc)
    //TODO run it!
  }
}

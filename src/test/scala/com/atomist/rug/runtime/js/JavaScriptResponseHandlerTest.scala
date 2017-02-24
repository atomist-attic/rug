package com.atomist.rug.runtime.js

import com.atomist.param.{SimpleParameterValue, SimpleParameterValues}
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.runtime.InstructionResponse
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.TreeMaterializer
import org.scalatest.{FlatSpec, Matchers}

class JavaScriptResponseHandlerTest extends FlatSpec with Matchers{
  val atomistConfig: AtomistConfig = DefaultAtomistConfig
  val treeMaterializer: TreeMaterializer = TestTreeMaterializer

  val kitties = "Kitties"
  val kittyDesc = "Prints out kitty urls"
  val simpleResponseHandler =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
      |import {Respond, Instruction, Response, Plan, Message} from '@atomist/rug/operations/Handlers'
      |import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
      |import {EventHandler, ResponseHandler, CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
      |import {Project} from '@atomist/rug/model/Core'
      |import {HandleResponse, HandleEvent, HandleCommand} from '@atomist/rug/operations/Handlers'
      |
      |@ResponseHandler("$kitties", "$kittyDesc")
      |@Tags("kitties", "ftw")
      |class KittiesResponder implements HandleResponse<Object>{
      |
      |  @Parameter({description: "his dudeness", pattern: "^.*$$"})
      |  name: string = "dude"
      |
      |  handle(response: Response<Object>) : Message {
      |
      |    if(this.name != "his dudeness") throw new Error("Not on the rug, man!");
      |    let results = response.body as any;
      |    return new Message("https://www.youtube.com/watch?v=fNodQpGVVyg")
      |  }
      |}
      |
      |export let kit = new KittiesResponder();
      |
    """.stripMargin)

  it should "extract and run a response handler" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleResponseHandler))
    val finder = new JavaScriptResponseHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    handler.name should be(kitties)
    handler.description should be (kittyDesc)
    handler.tags.size should be (2)
    val response = InstructionResponse("It worked! :p", 204, "woot")
    val plan = handler.handle(response, SimpleParameterValues(SimpleParameterValue("name","his dudeness")))
    //TODO validate the plan
  }
}

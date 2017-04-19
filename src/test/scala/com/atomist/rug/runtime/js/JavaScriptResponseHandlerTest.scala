package com.atomist.rug.runtime.js

import com.atomist.param.{SimpleParameterValue, SimpleParameterValues}
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.spi.Handlers.{Response, Status}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.TreeMaterializer
import org.scalatest.{FlatSpec, Matchers}

class JavaScriptResponseHandlerTest extends FlatSpec with Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig
  val treeMaterializer: TreeMaterializer = TestTreeMaterializer

  val kitties = "Kitties"
  val kittyDesc = "Prints out kitty urls"
  val simpleResponseHandler =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
      |import {Respond, Instruction, Response, CommandPlan, DirectedMessage, UserAddress} from '@atomist/rug/operations/Handlers'
      |import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
      |import {EventHandler, ResponseHandler, CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
      |import {Project} from '@atomist/rug/model/Core'
      |import {HandleResponse, HandleEvent, HandleCommand} from '@atomist/rug/operations/Handlers'
      |
      |@ResponseHandler("$kitties", "$kittyDesc")
      |@Tags("kitties", "ftw")
      |class KittiesResponder implements HandleResponse<String>{
      |
      |  @Parameter({description: "his dudeness", pattern: "^.*$$"})
      |  name: string = "dude"
      |
      |  handle(response: Response<String>) {
      |    if(this.name != "his dudeness") throw new Error("Not on the rug, man!");
      |    let results = response.body;
      |    if(results != "woot") {
      |       throw new Error("This will not stand");
      |    }
      |    return new CommandPlan().add(new DirectedMessage("https://www.youtube.com/watch?v=fNodQpGVVyg", new UserAddress("bob")));
      |  }
      |}
      |
      |export let kit = new KittiesResponder();
      |
    """.stripMargin)

  val simpleResponseHandlerWithJsonCoercion =  StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    s"""
       |import {Respond, Instruction, Response, CommandPlan} from '@atomist/rug/operations/Handlers'
       |import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
       |import {EventHandler, ParseJson, ResponseHandler, CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
       |import {Project} from '@atomist/rug/model/Core'
       |import {HandleResponse, HandleEvent, HandleCommand} from '@atomist/rug/operations/Handlers'
       |
       |@ResponseHandler("$kitties", "$kittyDesc")
       |class KittiesResponder implements HandleResponse<any>{
       | handle(@ParseJson response: Response<any>) : CommandPlan {
       |    let results = response.body as any;
       |
       |    let stringed = "Thing " + response;
       |    if (stringed === "Thing null")
       |      throw new Error("Response is evaluating to null default string");
       |
       |    if(results.yaml != "is more annoying than json") { throw new Error("Rats: " + results.yaml)}
       |    results.reasons.map(reason => reason.main.length)
       |    return new CommandPlan();
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
    val response = Response(Status.Success, Some("It worked! :p"), Some(204), Some("woot"))
    val plan = handler.handle(response, SimpleParameterValues(SimpleParameterValue("name","his dudeness")))
    //TODO validate the plan
  }

  it should "coerce responses to json if they are strings or bytes and the annotation is present" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleResponseHandlerWithJsonCoercion))
    val finder = new JavaScriptResponseHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    val response = Response(Status.Success, Some("It worked! :p"), Some(204), Some(
      """
        |{
        |  "yaml" : "is more annoying than json",
        |  "reasons": [{"main" : "because it's hard to parse, and annoying to write"}]
        |}
      """.stripMargin))
    val plan = handler.handle(response, SimpleParameterValues.Empty)
  }
}

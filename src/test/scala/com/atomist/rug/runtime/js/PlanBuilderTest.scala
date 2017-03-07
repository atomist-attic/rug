package com.atomist.rug.runtime.js

import com.atomist.param.SimpleParameterValues
import com.atomist.project.archive.{DefaultAtomistConfig, RugArchiveReader}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers, OneInstancePerTest}

class PlanBuilderTest extends FunSpec with Matchers with OneInstancePerTest with MockitoSugar  {

  val simpleCommandWithObjectInstructionParamAsJson =
    StringFileArtifact(DefaultAtomistConfig.handlersRoot + "/Handler.ts",
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
        |    result.add({instruction: {kind: "execute", name: "ExampleFunction", parameters: {jsonparam: {mucho: "coolness"}}},
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

  it ("should serialize complex instruction parameters to json during plan building") {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandWithObjectInstructionParamAsJson))
    val rugs = RugArchiveReader.find(rugArchive, Nil)
    val com = rugs.commandHandlers.head
    val plan = com.handle(null,SimpleParameterValues.Empty).get
    assert(plan.instructions.head.instruction.detail.parameters.head.getValue === """{"mucho":"coolness"}""")
  }
}

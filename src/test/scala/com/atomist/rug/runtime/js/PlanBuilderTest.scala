package com.atomist.rug.runtime.js

import com.atomist.param._
import com.atomist.project.archive._
import com.atomist.rug.RugArchiveReader
import com.atomist.rug.runtime._
import com.atomist.rug.runtime.plans.{LocalInstructionRunner, LocalPlanRunner, PlanResultInterpreter, TestSecretResolver}
import com.atomist.rug.spi.Handlers.Status.Success
import com.atomist.rug.spi.Secret
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers, OneInstancePerTest}

import scala.concurrent.Await
import scala.concurrent.duration._

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
        |    result.add({instruction: {kind: "execute", name: "ExampleFunction", parameters: {jsonparam: {mucho: "coolness"}}}});
        |    return result;
        |  }
        |}
        |
        |export let command = new KittieFetcher();
        |
      """.stripMargin)

  val simpleCommandHandlerWithNullAndUndefinedParameterValues =
    StringFileArtifact(DefaultAtomistConfig.handlersRoot + "/Handler.ts",
      s"""
        |import {HandleCommand, HandleResponse, Message, Instruction, Response, HandlerContext, Plan} from '@atomist/rug/operations/Handlers'
        |import {CommandHandler, ResponseHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
        |
        |@CommandHandler("show-me-the-kitties","Search Youtube for kitty videos and post results to slack")
        |@Tags("kitty", "youtube", "slack")
        |@Intent("show me kitties","cats please")
        |class KittieFetcher implements HandleCommand{
        |
        |  handle(ctx: HandlerContext) : Plan {
        |    let result = new Plan()
        |    result.add({instruction: {kind: "execute", name: "ExampleFunction", parameters: {thingy: "blah"}},
        |                onSuccess: {name: "simple-response-handler", kind: "respond", parameters: {one: null, two: undefined}} });
        |    return result;
        |  }
        |}
        |
        |@ResponseHandler("simple-response-handler", "Checks response is equal to passed in parameter")
        |class Responder implements HandleResponse<String> {
        |
        |  @Parameter({description: "blah", pattern: "@any", required: false})
        |  one: string = null
        |
        |  @Parameter({description: "blah", pattern: "@any", required: false})
        |  two: string
        |
        |  handle(response: Response<string>) : Plan {
        |
        |    if(this.one != null) {
        |       throw new Error("One is not null: " + this.one)
        |    }
        |
        |    if(this.two != undefined) {
        |       throw new Error("Two is not undefined: " + this.two)
        |    }
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
    val rugs = RugArchiveReader(rugArchive)
    val com = rugs.commandHandlers.head
    val plan = com.handle(null,SimpleParameterValues.Empty).get
    assert(plan.instructions.head.instruction.detail.parameters.head.getValue === """{"mucho":"coolness"}""")
  }

  it ("should not pass through null/undefined parameters or JSON serialize them") {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandlerWithNullAndUndefinedParameterValues))
    val coord = Coordinate("com.atomist.test","test-rugs", "1.2.3")
    val resolver = new RugResolver(Dependency(rugArchive, Some(coord)))
    val com = resolver.resolvedDependencies.rugs.commandHandlers.head
    val plan = com.handle(null,SimpleParameterValues.Empty).get
    val runner = new LocalPlanRunner(null, new LocalInstructionRunner(com, null, null, new TestSecretResolver(com) {
      override def resolveSecrets(secrets: Seq[Secret]): Seq[ParameterValue] = {
        Seq(SimpleParameterValue("very", "cool"))
      }
    }, rugResolver = Some(resolver)))
    val result = Await.result(runner.run(plan, None), 10.seconds)
    assert(PlanResultInterpreter.interpret(result).status == Success)
  }
}

package com.atomist.rug.runtime.plans

import com.atomist.param.SimpleParameterValues
import com.atomist.project.archive.{DefaultAtomistConfig, RugArchiveReader}
import com.atomist.rug.spi.Handlers.Instruction._
import com.atomist.rug.spi.Handlers.Status._
import com.atomist.rug.spi.Handlers._
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mockito.MockitoSugar
import org.scalatest._
import org.slf4j.Logger

import org.mockito.Matchers.{eq => expected}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class LocalPlanRunnerTest extends FunSpec with Matchers with OneInstancePerTest with MockitoSugar  {

  val messageDeliverer = mock[MessageDeliverer]
  val instructionRunner = mock[InstructionRunner]
  val nestedPlanRunner = mock[PlanRunner]
  val logger = mock[Logger]

  val planRunner = new LocalPlanRunner(messageDeliverer, instructionRunner, Some(nestedPlanRunner), Some(logger))

  it ("should run empty plan") {
    val plan = Plan(Nil, Nil)

    val actualPlanResult = Await.result(planRunner.run(plan, None), 10.seconds)
    val expectedPlanResponse = PlanResult(Seq())
    assert(actualPlanResult == expectedPlanResponse)

    verifyNoMoreInteractions(messageDeliverer, instructionRunner, nestedPlanRunner, logger)
  }

  it ("should run a fully populated plan") {
    val plan = Plan(
        Seq(
          Message(
            MessageText("message1"),
            Seq(Presentable(Generate(Detail("generate1", None, Nil, None)), Some("label1"))),
            None
          )
        ),
        Seq(
          Respondable(
            Edit(Detail("edit1", None, Nil, None)),
            None,
            None
          ),
          Respondable(
            Edit(Detail("edit2", None, Nil, None)),
            Some(Message(MessageText("pass"), Nil, None)),
            Some(Message(MessageText("fail"), Nil, None))
          ),
          Respondable(
            Edit(Detail("edit3", None, Nil, None)),
            Some(Respond(Detail("respond1", None, Nil, None))),
            None
          ),
          Respondable(
            Edit(Detail("edit4", None, Nil, None)),
            Some(Plan(Seq(Message(MessageText("nested plan"), Nil, None)), Nil)),
            None
          )
        )
      )
    val instructionNameAsSuccessResponseBody = new Answer[Response]() {
      def answer(invocation: InvocationOnMock) = {
        val input = invocation.getArgumentAt(0, classOf[Instruction]).detail.name
        Response(Success, None, Some(0), Some(input))
      }
    }
    when(instructionRunner.run(any(), any())).thenAnswer(instructionNameAsSuccessResponseBody)
    when(nestedPlanRunner.run(any(), any())).thenReturn(Future {
      PlanResult(Seq(MessageDeliveryError(Message(MessageText("nested plan"), Nil, None), null)))
    })

    val actualPlanResult = Await.result(planRunner.run(plan, None), 10.seconds)
    val expectedPlanLog = Set(
      InstructionResult(Edit(Detail("edit1", None, Nil, None)), Response(Success, None, Some(0), Some("edit1"))),
      InstructionResult(Edit(Detail("edit2", None, Nil, None)), Response(Success, None, Some(0), Some("edit2"))),
      InstructionResult(Edit(Detail("edit3", None, Nil, None)), Response(Success, None, Some(0), Some("edit3"))),
      InstructionResult(Edit(Detail("edit4", None, Nil, None)), Response(Success, None, Some(0), Some("edit4"))),
      InstructionResult(Respond(Detail("respond1", None, Nil, None)),Response(Success,None,Some(0),Some("respond1"))),
      NestedPlanRun(Plan(List(Message(MessageText("nested plan"), Nil, None)), Nil),
        Future(PlanResult(List(MessageDeliveryError(Message(MessageText("nested plan"), Nil, None), null)))))
      )

    assert(makeEventsComparable(actualPlanResult.log.toSet) == makeEventsComparable(expectedPlanLog))

    verify(messageDeliverer).deliver(
      Message(MessageText("message1"), Seq(Presentable(Generate(Detail("generate1", None, Nil, None)), Some("label1"))), None),
      None)
    verify(instructionRunner).run(Edit(Detail("edit1", None, Nil, None)), None)
    verify(instructionRunner).run(Edit(Detail("edit2", None, Nil, None)), None)
    verify(messageDeliverer).deliver(Message(MessageText("pass"), Nil, None), Some(Response(Success,None,Some(0),Some("edit2"))))
    verify(instructionRunner).run(Edit(Detail("edit3", None, Nil, None)), None)
    verify(instructionRunner).run(Respond(Detail("respond1", None, Nil, None)), Some(Response(Success, None, Some(0), Some("edit3"))))
    verify(instructionRunner).run(Edit(Detail("edit4", None, Nil, None)), None)
    verify(nestedPlanRunner).run(Plan(Seq(Message(MessageText("nested plan"), Nil, None)), Nil), Some(Response(Success,None,Some(0),Some("edit4"))))

    verify(logger).debug("Delivered message: MessageText(message1)")
    verify(logger).debug("Ran instruction: Edit(Detail(edit1,None,List(),None)) and got response: Response(Success,None,Some(0),Some(edit1))")
    verify(logger).debug("Ran instruction: Edit(Detail(edit2,None,List(),None)) and got response: Response(Success,None,Some(0),Some(edit2))")
    verify(logger).debug("Ran instruction: Edit(Detail(edit3,None,List(),None)) and got response: Response(Success,None,Some(0),Some(edit3))")
    verify(logger).debug("Ran instruction: Edit(Detail(edit4,None,List(),None)) and got response: Response(Success,None,Some(0),Some(edit4))")
    verify(logger).debug("Ran instruction: Respond(Detail(respond1,None,List(),None)) and got response: Response(Success,None,Some(0),Some(respond1))")
    verify(logger).debug("Ran Message(MessageText(pass),List(),None) after Edit(Detail(edit2,None,List(),None))")
    verify(logger).debug("Ran Respond(Detail(respond1,None,List(),None)) after Edit(Detail(edit3,None,List(),None))")
    verify(logger).debug("Ran Plan(List(Message(MessageText(nested plan),List(),None)),List()) after Edit(Detail(edit4,None,List(),None))")

    verifyNoMoreInteractions(messageDeliverer, instructionRunner, nestedPlanRunner, logger)
  }

  it ("should run a plan with failing response") {
    val plan = Plan(Nil,
      Seq(
        Respondable(
          Edit(Detail("edit2", None, Nil, None)),
          Some(Message(MessageText("pass"), Nil, None)),
          Some(Message(MessageText("fail"), Nil, None))
        )
      )
    )
    val instructionNameAsFailureResponseBody = new Answer[Response]() {
      def answer(invocation: InvocationOnMock) = {
        val input = invocation.getArgumentAt(0, classOf[Instruction]).detail.name
        Response(Failure, None, Some(0), Some(input))
      }
    }
    when(instructionRunner.run(any(), any())).thenAnswer(instructionNameAsFailureResponseBody)

    val actualPlanResult = Await.result(planRunner.run(plan, None), 120.seconds)
    val expectedPlanLog = Set(
      InstructionResult(Edit(Detail("edit2", None, Nil, None)), Response(Failure, None, Some(0), Some("edit2")))
    )
    assert(actualPlanResult.log.toSet == expectedPlanLog)

    val inOrder = Mockito.inOrder(messageDeliverer, instructionRunner, nestedPlanRunner)
    inOrder.verify(instructionRunner).run(Edit(Detail("edit2", None, Nil, None)), None)
    inOrder.verify(messageDeliverer).deliver(Message(MessageText("fail"), Nil, None ), Some(Response(Failure, None, Some(0), Some("edit2"))))

    verify(logger).debug("Ran instruction: Edit(Detail(edit2,None,List(),None)) and got response: Response(Failure,None,Some(0),Some(edit2))")
    verify(logger).debug("Ran Message(MessageText(fail),List(),None) after Edit(Detail(edit2,None,List(),None))")

    verifyNoMoreInteractions(messageDeliverer, instructionRunner, nestedPlanRunner, logger)
  }

  val makeEventsComparable = (log: Iterable[PlanLogEvent]) => PlanUtils.drawEventLogs("events", log.toSeq)

  it ("should handle error during message delivery") {
    val plan = Plan(
      Seq(
        Message(
          MessageText("message1"),
          Nil,
          None
        )
      ),
      Nil
    )
    when(messageDeliverer.deliver(any(), any())).thenThrow(new IllegalArgumentException("Uh oh!"))

    val actualPlanResult = Await.result(planRunner.run(plan, None), 10.seconds)
    val expectedPlanLog = Set(
      MessageDeliveryError(Message(MessageText("message1"), Nil, None), new IllegalArgumentException("Uh oh!"))
    )
    assert(makeEventsComparable(actualPlanResult.log.toSet) == makeEventsComparable(expectedPlanLog))

    verify(logger).error(expected("Failed to deliver message: MessageText(message1) - Uh oh!"), any(classOf[Throwable]))

    verifyNoMoreInteractions(instructionRunner, nestedPlanRunner, logger)
  }

  it ("should handle error during respondable instruction execution") {
    val plan = Plan(
      Nil,
      Seq(
        Respondable(
          Edit(Detail("fail", None, Nil, None)),
          None,
          None
        )
      )
    )
    when(instructionRunner.run(any(), any())).thenThrow(new IllegalArgumentException("Uh oh!"))

    val actualPlanResult = Await.result(planRunner.run(plan, None), 10.seconds)
    val expectedPlanLog = Set(
      InstructionError(Edit(Detail("fail", None, Nil, None)), new IllegalArgumentException("Uh oh!"))
    )
    assert(makeEventsComparable(actualPlanResult.log.toSet) == makeEventsComparable(expectedPlanLog))

    verify(logger).error(expected("Failed to run instruction: Edit(Detail(fail,None,List(),None)) - Uh oh!"), any(classOf[Throwable]))

    verifyNoMoreInteractions(messageDeliverer, nestedPlanRunner, logger)
  }

  it ("should handle failing respondable callback") {
    val plan = Plan(
      Nil,
      Seq(
        Respondable(
          Edit(Detail("edit", None, Nil, None)),
          Some(Plan(Seq(Message(MessageText("fail"), Nil, None)), Nil)),
          None
        )
      )
    )
    when(instructionRunner.run(any(), any())).thenReturn(Response(Success, None, Some(0), None))
    when(nestedPlanRunner.run(any(), any())).thenThrow(new IllegalStateException("Uh oh!"))

    val actualPlanResult = Await.result(planRunner.run(plan, None), 10.seconds)
    val expectedPlanLog = Set(
      InstructionResult(Edit(Detail("edit", None, Nil, None)), Response(Success, None, Some(0), None)),
      CallbackError(Plan(List(Message(MessageText("fail"), Nil, None)), Nil), new IllegalArgumentException("Uh oh!"))
    )
    assert(makeEventsComparable(actualPlanResult.log.toSet) == makeEventsComparable(expectedPlanLog))

    verify(logger).debug("Ran instruction: Edit(Detail(edit,None,List(),None)) and got response: Response(Success,None,Some(0),None)")
    verify(logger).error(expected("Failed to run Plan(List(Message(MessageText(fail),List(),None)),List()) after Edit(Detail(edit,None,List(),None)) - Uh oh!"), any(classOf[Throwable]))

    verifyNoMoreInteractions(messageDeliverer, logger)
  }
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

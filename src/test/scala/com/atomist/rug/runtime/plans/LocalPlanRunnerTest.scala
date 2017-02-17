package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers.Instruction._
import com.atomist.rug.spi.Handlers.Status._
import com.atomist.rug.spi.Handlers._
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mockito.MockitoSugar
import org.scalatest._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class LocalPlanRunnerTest extends FunSpec with Matchers with DiagrammedAssertions with OneInstancePerTest with MockitoSugar  {

  val messageDeliverer = mock[MessageDeliverer]
  val instructionRunner = mock[InstructionRunner]
  val nestedPlanRunner = mock[PlanRunner]

  val planRunner = new LocalPlanRunner(messageDeliverer, instructionRunner, Some(nestedPlanRunner))

  it ("should run empty plan") {
    val plan = Plan(Nil, Nil)

    val actualPlanResult = Await.result(planRunner.run(plan, "plan input"), 10.seconds)
    val expectedPlanResponse = PlanResult(Seq())
    assert(actualPlanResult == expectedPlanResponse)

    verifyNoMoreInteractions(messageDeliverer, instructionRunner, nestedPlanRunner)
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

    val actualPlanResult = Await.result(planRunner.run(plan, "plan input"), 10.seconds)
    val expectedPlanLog = Set(
      InstructionResponse(Edit(Detail("edit1", None, Nil, None)), Response(Success, None, Some(0), Some("edit1"))),
      InstructionResponse(Edit(Detail("edit2", None, Nil, None)), Response(Success, None, Some(0), Some("edit2"))),
      InstructionResponse(Edit(Detail("edit3", None, Nil, None)), Response(Success, None, Some(0), Some("edit3"))),
      InstructionResponse(Edit(Detail("edit4", None, Nil, None)), Response(Success, None, Some(0), Some("edit4"))),
      InstructionResponse(Respond(Detail("respond1", None, Nil, None)),Response(Success,None,Some(0),Some("respond1"))),
      NestedPlanRun(Plan(List(Message(MessageText("nested plan"), Nil, None)), Nil),
        Future(PlanResult(List(MessageDeliveryError(Message(MessageText("nested plan"), Nil, None), null)))))
      )

    assert(makeEventsComparable(actualPlanResult.log.toSet) == makeEventsComparable(expectedPlanLog))

    verify(messageDeliverer).deliver(
      Message(MessageText("message1"), Seq(Presentable(Generate(Detail("generate1", None, Nil, None)), Some("label1"))), None),
      "plan input")
    verify(instructionRunner).run(Edit(Detail("edit1", None, Nil, None)), "plan input")
    verify(instructionRunner).run(Edit(Detail("edit2", None, Nil, None)), "plan input")
    verify(messageDeliverer).deliver(Message(MessageText("pass"), Nil, None), "edit2")
    verify(instructionRunner).run(Edit(Detail("edit3", None, Nil, None)), "plan input")
    verify(instructionRunner).run(Respond(Detail("respond1", None, Nil, None)), "edit3")
    verify(instructionRunner).run(Edit(Detail("edit4", None, Nil, None)), "plan input")
    verify(nestedPlanRunner).run(Plan(Seq(Message(MessageText("nested plan"), Nil, None)), Nil), "edit4")
    verifyNoMoreInteractions(messageDeliverer, instructionRunner, nestedPlanRunner)
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

    val actualPlanResult = Await.result(planRunner.run(plan, "plan input"), 10.seconds)
    val expectedPlanLog = Set(
      InstructionResponse(Edit(Detail("edit2", None, Nil, None)), Response(Failure, None, Some(0), Some("edit2")))
    )
    assert(actualPlanResult.log.toSet == expectedPlanLog)

    val inOrder = Mockito.inOrder(messageDeliverer, instructionRunner, nestedPlanRunner)
    inOrder.verify(instructionRunner).run(Edit(Detail("edit2", None, Nil, None)), "plan input")
    inOrder.verify(messageDeliverer).deliver(Message(MessageText("fail"), Nil, None), "edit2")
    verifyNoMoreInteractions(messageDeliverer, instructionRunner, nestedPlanRunner)
  }

  val makeEventsComparable = (log: Iterable[PlanLogEvent]) => log.map {
    case InstructionResponse(i, r) => (i, r)
    case NestedPlanRun(p, f) => (p, f.value.get)
    case InstructionError(i, e) => (i, e.getMessage)
    case MessageDeliveryError(m, e) => (m, e.getMessage)
    case CallbackError(c, e) => (c, e.getMessage)
  }

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

    val actualPlanResult = Await.result(planRunner.run(plan, "plan input"), 10.seconds)
    val expectedPlanLog = Set(
      MessageDeliveryError(Message(MessageText("message1"), Nil, None), new IllegalArgumentException("Uh oh!"))
    )
    assert(makeEventsComparable(actualPlanResult.log.toSet) == makeEventsComparable(expectedPlanLog))
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

    val actualPlanResult = Await.result(planRunner.run(plan, "plan input"), 10.seconds)
    val expectedPlanLog = Set(
      InstructionError(Edit(Detail("fail", None, Nil, None)), new IllegalArgumentException("Uh oh!"))
    )
    assert(makeEventsComparable(actualPlanResult.log.toSet) == makeEventsComparable(expectedPlanLog))
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

    val actualPlanResult = Await.result(planRunner.run(plan, "plan input"), 10.seconds)
    val expectedPlanLog = Set(
      InstructionResponse(Edit(Detail("edit", None, Nil, None)), Response(Success, None, Some(0), None)),
      CallbackError(Plan(List(Message(MessageText("fail"), Nil, None)), Nil), new IllegalArgumentException("Uh oh!"))
    )
    assert(makeEventsComparable(actualPlanResult.log.toSet) == makeEventsComparable(expectedPlanLog))
  }

}

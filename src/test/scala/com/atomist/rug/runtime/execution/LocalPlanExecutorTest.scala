package com.atomist.rug.runtime.execution

import com.atomist.rug.spi.Handlers.Instruction._
import com.atomist.rug.spi.Handlers.Status._
import com.atomist.rug.spi.Handlers._
import com.atomist.rug.spi.JavaHandlers
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mockito.MockitoSugar
import org.scalatest._
import com.atomist.rug.spi.JavaHandlersConverter._
import com.atomist.rug.spi.JavaHandlers.{Response => JavaResponse}

class LocalPlanExecutorTest extends FunSpec with Matchers with DiagrammedAssertions with OneInstancePerTest with MockitoSugar  {

  val messageDeliverer = mock[MessageDeliverer]
  val instructionExecutor = mock[InstructionExecutor]
  val nestedPlanExecutor = mock[PlanExecutor]

  val planExecuter = new LocalPlanExecutor(messageDeliverer, instructionExecutor, Some(nestedPlanExecutor))

  it ("should execute empty plan") {
    val plan = Plan(Nil, Nil)

    val actualPlanResponse = planExecuter.execute(plan, "plan input")
    val expectedPlanResponse = PlanResponse(Map(), Map(), Map(), Map())
    assert(actualPlanResponse == expectedPlanResponse)

    verifyNoMoreInteractions(messageDeliverer, instructionExecutor, nestedPlanExecutor)
  }

  it ("should execute a fully populated plan") {
    val plan = Plan(
        Seq(
          Message(
            MessageText("message1"),
            Seq(Presentable(Generate(Detail("generate1", None, Nil)), Some("label1"))),
            None
          )
        ),
        Seq(
          Respondable(
            Edit(Detail("edit1", None, Nil)),
            None,
            None
          ),
          Respondable(
            Edit(Detail("edit2", None, Nil)),
            Some(Message(MessageText("pass"), Nil, None)),
            Some(Message(MessageText("fail"), Nil, None))
          ),
          Respondable(
            Edit(Detail("edit3", None, Nil)),
            Some(Respond(Detail("respond1", None, Nil))),
            None
          ),
          Respondable(
            Edit(Detail("edit4", None, Nil)),
            Some(Plan(Seq(Message(MessageText("nested plan"), Nil, None)), Nil)),
            None
          )
        )
      )
    val instructionNameAsSuccessResponseBody = new Answer[JavaHandlers.Response]() {
      def answer(invocation: InvocationOnMock) = {
        val input = invocation.getArgumentAt(0, classOf[JavaHandlers.Instruction]).name
        JavaHandlers.Response(Success.toString, null, 0, input)
      }
    }
    when(instructionExecutor.execute(any(), any())).thenAnswer(instructionNameAsSuccessResponseBody)

    val actualPlanResponse = planExecuter.execute(plan, "plan input")
    val expectedPlanResponse = PlanResponse(
      Map(
        Edit(Detail("edit1", None, List())) -> Response(Success, None, Some(0), Some("edit1")),
        Edit(Detail("edit2", None, List())) -> Response(Success, None, Some(0), Some("edit2")),
        Edit(Detail("edit3", None, List())) -> Response(Success, None, Some(0), Some("edit3")),
        Edit(Detail("edit4", None, List())) -> Response(Success, None, Some(0), Some("edit4"))
      ),
      Map(),Map(),Map()
    )

    assert(actualPlanResponse == expectedPlanResponse)

    verify(messageDeliverer).deliver(toJavaMessage(
      Message(MessageText("message1"), Seq(Presentable(Generate(Detail("generate1", None, Nil)), Some("label1"))), None)),
      "plan input")
    verify(instructionExecutor).execute(toJavaInstruction(Edit(Detail("edit1", None, Nil))), "plan input")
    verify(instructionExecutor).execute(toJavaInstruction(Edit(Detail("edit2", None, Nil))), "plan input")
    verify(messageDeliverer).deliver(toJavaMessage(Message(MessageText("pass"), Nil, None)), "edit2")
    verify(instructionExecutor).execute(toJavaInstruction(Edit(Detail("edit3", None, Nil))), "plan input")
    verify(instructionExecutor).execute(toJavaInstruction(Respond(Detail("respond1", None, Nil))), "edit3")
    verify(instructionExecutor).execute(toJavaInstruction(Edit(Detail("edit4", None, Nil))), "plan input")
    verify(nestedPlanExecutor).execute(Plan(Seq(Message(MessageText("nested plan"), Nil, None)), Nil), "edit4")
    verifyNoMoreInteractions(messageDeliverer, instructionExecutor, nestedPlanExecutor)
  }

  it ("should execute a plan with failing response") {
    val plan = Plan(Nil,
      Seq(
        Respondable(
          Edit(Detail("edit2", None, Nil)),
          Some(Message(MessageText("pass"), Nil, None)),
          Some(Message(MessageText("fail"), Nil, None))
        )
      )
    )
    val instructionNameAsFailureResponseBody = new Answer[JavaHandlers.Response]() {
      def answer(invocation: InvocationOnMock) = {
        val input = invocation.getArgumentAt(0, classOf[JavaHandlers.Instruction]).name
        JavaHandlers.Response(Failure.toString, null, 0, input)
      }
    }
    when(instructionExecutor.execute(any(), any())).thenAnswer(instructionNameAsFailureResponseBody)

    val actualPlanResponse = planExecuter.execute(plan, "plan input")
    val expectedPlanResponse = PlanResponse(Map(
      Edit(Detail("edit2", None, List())) -> Response(Failure, None, Some(0), Some("edit2"))),
      Map(),Map(),Map()
    )
    assert(actualPlanResponse == expectedPlanResponse)

    val inOrder = Mockito.inOrder(messageDeliverer, instructionExecutor, nestedPlanExecutor)
    inOrder.verify(instructionExecutor).execute(toJavaInstruction(Edit(Detail("edit2", None, Nil))), "plan input")
    inOrder.verify(messageDeliverer).deliver(toJavaMessage(Message(MessageText("fail"), Nil, None)), "edit2")
    verifyNoMoreInteractions(messageDeliverer, instructionExecutor, nestedPlanExecutor)
  }

  val makeErrorsComparable = (errorMap: Map[_ <: Any, Throwable]) => errorMap.map{ case (k, v) => (k, v.getMessage)}

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

    val actualPlanResponse = planExecuter.execute(plan, "plan input")
    val expectedErrors = Map(Message(MessageText("message1"), List(), None) -> new IllegalArgumentException("Uh oh!"))
    assert(actualPlanResponse.instructionResponses == Map())
    assert(makeErrorsComparable(actualPlanResponse.messageDeliveryErrors) == makeErrorsComparable(expectedErrors))
    assert(actualPlanResponse.instructionErrors == Map())
    assert(actualPlanResponse.callbackErrors == Map())
  }

  it ("should handle error during respondable instruction execution") {
    val plan = Plan(
      Nil,
      Seq(
        Respondable(
          Edit(Detail("fail", None, Nil)),
          None,
          None
        )
      )
    )
    when(instructionExecutor.execute(any(), any())).thenThrow(new IllegalArgumentException("Uh oh!"))

    val actualPlanResponse = planExecuter.execute(plan, "plan input")
    val expectedErrors = Map(Edit(Detail("fail", None, List())) -> new IllegalArgumentException("Uh oh!"))
    assert(actualPlanResponse.instructionResponses == Map())
    assert(actualPlanResponse.messageDeliveryErrors == Map())
    assert(makeErrorsComparable(actualPlanResponse.instructionErrors) == makeErrorsComparable(expectedErrors))
    assert(actualPlanResponse.callbackErrors == Map())
  }

  it ("should handle failing respondable callback") {
    val plan = Plan(
      Nil,
      Seq(
        Respondable(
          Edit(Detail("edit", None, Nil)),
          Some(Plan(Seq(Message(MessageText("fail"), Nil, None)), Nil)),
          None
        )
      )
    )
    when(instructionExecutor.execute(any(), any())).thenReturn(JavaResponse(Success.toString, null, 0, null))
    when(nestedPlanExecutor.execute(any(), any())).thenThrow(new IllegalStateException("Uh oh!"))

    val actualPlanResponse = planExecuter.execute(plan, "plan input")
    val expectedErrors = Map(Plan(List(Message(MessageText("fail"), List(), None)), List()) -> new IllegalStateException("Uh oh!"))
    assert(actualPlanResponse.instructionResponses == Map(Edit(Detail("edit", None, List())) -> Response(Success, None, Some(0), None)))
    assert(actualPlanResponse.messageDeliveryErrors == Map())
    assert(actualPlanResponse.instructionErrors == Map())
    assert(makeErrorsComparable(actualPlanResponse.callbackErrors) == makeErrorsComparable(expectedErrors))
  }

}

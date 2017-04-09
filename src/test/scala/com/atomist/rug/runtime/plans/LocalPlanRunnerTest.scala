package com.atomist.rug.runtime.plans

import com.atomist.param.{ParameterValues, SimpleParameterValues, Tag}
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.edit.{Applicability, ModificationAttempt, ProjectEditor}
import com.atomist.rug.spi.Handlers.Instruction._
import com.atomist.rug.spi.Handlers.Status._
import com.atomist.rug.spi.Handlers._
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
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

class LocalPlanRunnerTest extends FunSpec with Matchers with OneInstancePerTest with MockitoSugar with DiagrammedAssertions {

  val messageDeliverer = mock[MessageDeliverer]
  val instructionRunner = mock[InstructionRunner]
  val nestedPlanRunner = mock[PlanRunner]
  val editor = mock[ProjectEditor]
  val logger = mock[Logger]

  val planRunner = new LocalPlanRunner(messageDeliverer, instructionRunner, Some(nestedPlanRunner), Some(logger))

  it ("should run empty plan") {
    val plan = Plan(null, Nil, Nil, Nil)

    val actualPlanResult = Await.result(planRunner.run(plan, None), 10.seconds)
    val expectedPlanResponse = PlanResult(Seq())
    assert(actualPlanResult == expectedPlanResponse)

    verifyNoMoreInteractions(messageDeliverer, instructionRunner, nestedPlanRunner, logger)
  }

  it ("should run a fully populated plan") {
    val plan = Plan(
        Some(editor),
        Nil,
        Seq(LocallyRenderedMessage("message1", "text/plain", Nil, Nil)),
        Seq(
          Respondable(
            Edit(Detail("edit1", None, Nil, None)),
            None,
            None
          ),
          Respondable(
            Edit(Detail("edit2", None, Nil, None)),
            Some(LocallyRenderedMessage("pass", "text/plain", Nil, Nil)),
            Some(LocallyRenderedMessage("fail", "text/plain", Nil, Nil))
          ),
          Respondable(
            Edit(Detail("edit3", None, Nil, None)),
            Some(Respond(Detail("respond1", None, Nil, None))),
            None
          ),
          Respondable(
            Edit(Detail("edit4", None, Nil, None)),
            Some(Plan(None, Nil, Seq(LocallyRenderedMessage("nested plan", "text/plain", Nil, Nil)), Nil)),
            None
          )
        )
      )
    val instructionNameAsSuccessResponseBody = new Answer[Response]() {
      def answer(invocation: InvocationOnMock) = {
        val input = invocation.getArgumentAt(0, classOf[Instruction]).detail.name
        Response(Success, Some(input), Some(0), None)
      }
    }
    when(instructionRunner.run(any(), any())).thenAnswer(instructionNameAsSuccessResponseBody)
    when(nestedPlanRunner.run(any(), any())).thenReturn(Future {
      PlanResult(Seq(MessageDeliveryError(LocallyRenderedMessage("nested plan", "text/plain", Nil, Nil), null)))
    })

    val actualPlanResult = Await.result(planRunner.run(plan, None), 120.seconds)
    val expectedPlanLog = Set(
      InstructionResult(Edit(Detail("edit1", None, Nil, None)), Response(Success, Some("edit1"), Some(0), None)),
      InstructionResult(Edit(Detail("edit2", None, Nil, None)), Response(Success, Some("edit2"), Some(0), None)),
      InstructionResult(Edit(Detail("edit3", None, Nil, None)), Response(Success, Some("edit3"), Some(0), None)),
      InstructionResult(Edit(Detail("edit4", None, Nil, None)), Response(Success, Some("edit4"), Some(0), None)),
      InstructionResult(Respond(Detail("respond1", None, Nil, None)),Response(Success, Some("respond1"), Some(0), None)),
      NestedPlanRun(Plan(None, Nil, Seq(LocallyRenderedMessage("nested plan", "text/plain", Nil, Nil)), Nil),
        Future(PlanResult(List(MessageDeliveryError(LocallyRenderedMessage("nested plan", "text/plain", Nil, Nil), null)))))
      )

    assert(makeEventsComparable(actualPlanResult.log.toSet) == makeEventsComparable(expectedPlanLog))

    verify(messageDeliverer).deliver(
      editor,
      LocallyRenderedMessage("message1", "text/plain", Nil, Nil),
      None)
    verify(instructionRunner).run(Edit(Detail("edit1", None, Nil, None)), None)
    verify(instructionRunner).run(Edit(Detail("edit2", None, Nil, None)), None)
    verify(messageDeliverer).deliver(editor, LocallyRenderedMessage("pass", "text/plain", Nil, Nil), Some(Response(Success, Some("edit2"), Some(0), None)))
    verify(instructionRunner).run(Edit(Detail("edit3", None, Nil, None)), None)
    verify(instructionRunner).run(Respond(Detail("respond1", None, Nil, None)), Some(Response(Success, Some("edit3"), Some(0), None)))
    verify(instructionRunner).run(Edit(Detail("edit4", None, Nil, None)), None)
    verify(nestedPlanRunner).run(Plan(None, Nil, Seq(LocallyRenderedMessage("nested plan", "text/plain", Nil, Nil)), Nil), Some(Response(Success, Some("edit4"), Some(0), None)))


    verify(logger).debug("Delivered message channels: , usernames: , type: text/plain, body: message1")

    verify(logger).debug("Ran Edit edit1() and then Success:0:edit1")
    verify(logger).debug("Ran Edit edit2() and then Success:0:edit2")
    verify(logger).debug("Ran Edit edit3() and then Success:0:edit3")
    verify(logger).debug("Ran Edit edit4() and then Success:0:edit4")
    verify(logger).debug("Delivered message channels: , usernames: , type: text/plain, body: pass")
    verify(logger).debug("Ran Respond respond1() and then Success:0:respond1")
    verify(logger).debug("Invoked channels: , usernames: , type: text/plain, body: pass after Edit edit2()")
    verify(logger).debug("Invoked Respond respond1() after Edit edit3()")
    verify(logger).debug("Invoked Plan[channels: , usernames: , type: text/plain, body: nested plan] after Edit edit4()")

    verifyNoMoreInteractions(messageDeliverer, instructionRunner, nestedPlanRunner, logger)
  }

  it ("should run a plan with failing response") {
    val plan = Plan(Some(editor), Nil, Nil,
      Seq(
        Respondable(
          Edit(Detail("edit2", None, Nil, None)),
          Some(LocallyRenderedMessage("pass", "text/plain", Nil, Nil)),
          Some(LocallyRenderedMessage("fail", "text/plain", Nil, Nil))
        )
      )
    )
    val instructionNameAsFailureResponseBody = new Answer[Response]() {
      def answer(invocation: InvocationOnMock) = {
        val input = invocation.getArgumentAt(0, classOf[Instruction]).detail.name
        Response(Failure, Some(input), Some(0), None)
      }
    }
    when(instructionRunner.run(any(), any())).thenAnswer(instructionNameAsFailureResponseBody)

    val actualPlanResult = Await.result(planRunner.run(plan, None), 120.seconds)
    val expectedPlanLog = Set(
      InstructionResult(Edit(Detail("edit2", None, Nil, None)), Response(Failure, Some("edit2"), Some(0), None))
    )
    assert(actualPlanResult.log.toSet == expectedPlanLog)

    val inOrder = Mockito.inOrder(messageDeliverer, instructionRunner, nestedPlanRunner)
    inOrder.verify(instructionRunner).run(Edit(Detail("edit2", None, Nil, None)), None)
    inOrder.verify(messageDeliverer).deliver(editor, LocallyRenderedMessage("fail", "text/plain", Nil, Nil), Some(Response(Failure, Some("edit2"), Some(0), None)))

    verify(logger).debug("Ran Edit edit2() and then Failure:0:edit2")
    verify(logger).debug("Delivered message channels: , usernames: , type: text/plain, body: fail")
    verify(logger).debug("Invoked channels: , usernames: , type: text/plain, body: fail after Edit edit2()")

    verifyNoMoreInteractions(messageDeliverer, instructionRunner, nestedPlanRunner, logger)
  }

  val makeEventsComparable = (log: Iterable[PlanLogEvent]) => log.map {
    case InstructionResult(i, r) => (i, r)
    case NestedPlanRun(p, f) =>
      val r = Await.result(f, 10.seconds)
      (p, r)
    case InstructionError(i, e) => (i, e.getMessage)
    case MessageDeliveryError(m, e) => (m, e.getMessage)
    case CallbackError(c, e) => (c, e.getMessage)
  }

  it ("should handle error during message delivery") {
    val plan = Plan(Some(editor), Nil,
      Seq(
        LocallyRenderedMessage(
          "message1",
          "text/plain",
          Nil,
          Nil
        )
      ),
      Nil
    )
    when(messageDeliverer.deliver(any(), any(), any())).thenThrow(new IllegalArgumentException("Uh oh!"))

    val actualPlanResult = Await.result(planRunner.run(plan, None), 10.seconds)
    val expectedPlanLog = Set(
      MessageDeliveryError(LocallyRenderedMessage("message1", "text/plain", Nil, Nil), new IllegalArgumentException("Uh oh!"))
    )
    assert(makeEventsComparable(actualPlanResult.log.toSet) == makeEventsComparable(expectedPlanLog))

    verify(logger).error(expected("Failed to deliver message channels: , usernames: , type: text/plain, body: message1 - Uh oh!"), any(classOf[Throwable]))

    verifyNoMoreInteractions(instructionRunner, nestedPlanRunner, logger)
  }

  it ("should handle error during respondable instruction execution") {
    val plan = Plan(None, Nil,
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

    verify(logger).error(expected("Failed to run Edit fail() - Uh oh!"), any(classOf[Throwable]))

    verifyNoMoreInteractions(messageDeliverer, nestedPlanRunner, logger)
  }

  it ("should handle failing respondable callback") {
    val plan = Plan(None, Nil,
      Nil,
      Seq(
        Respondable(
          Edit(Detail("edit", None, Nil, None)),
          Some(Plan(None,Nil,Seq(LocallyRenderedMessage("fail", "text/plain", Nil, Nil)), Nil)),
          None
        )
      )
    )
    when(instructionRunner.run(any(), any())).thenReturn(Response(Success, None, None, None))
    when(nestedPlanRunner.run(any(), any())).thenThrow(new IllegalStateException("Uh oh!"))

    val actualPlanResult = Await.result(planRunner.run(plan, None), 10.seconds)
    val expectedPlanLog = Set(
      InstructionResult(Edit(Detail("edit", None, Nil, None)), Response(Success, None, None, None)),
      CallbackError(Plan(None, Nil, List(LocallyRenderedMessage("fail", "text/plain", Nil, Nil)), Nil), new IllegalArgumentException("Uh oh!"))
    )
    assert(makeEventsComparable(actualPlanResult.log.toSet) === makeEventsComparable(expectedPlanLog))

    verify(logger).debug("Ran Edit edit() and then Success")
    verify(logger).error(expected( "Failed to invoke Plan[channels: , usernames: , type: text/plain, body: fail] after Edit edit() - Uh oh!"), any(classOf[Throwable]))

    verifyNoMoreInteractions(messageDeliverer, logger)
  }
}

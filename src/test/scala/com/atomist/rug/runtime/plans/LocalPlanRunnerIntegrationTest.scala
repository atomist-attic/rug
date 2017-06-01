package com.atomist.rug.runtime.plans

import com.atomist.param._
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.project.edit.ProjectEditor
import com.atomist.rug.SimpleRugResolver
import com.atomist.rug.TestUtils.contentOf
import com.atomist.rug.runtime.CommandHandler
import com.atomist.rug.spi.Handlers.Instruction._
import com.atomist.rug.spi.Handlers.Status._
import com.atomist.rug.spi.Handlers._
import com.atomist.rug.spi.Secret
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.mockito.ArgumentMatchers.{eq => expected, _}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import org.slf4j.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class LocalPlanRunnerIntegrationTest
  extends FunSpec
    with Matchers
    with OneInstancePerTest
    with MockitoSugar
    with DiagrammedAssertions {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  val handlerThatHandlersAnError = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "HandlerThatHandlesAnError.ts"))

  it("should not return failure for handled errors") {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(handlerThatHandlersAnError))
    val resolver = SimpleRugResolver(rugArchive)

    val handlers = resolver.resolvedDependencies.resolvedRugs
    val handler = handlers.collect { case i: CommandHandler => i }.head
    val runner = new LocalPlanRunner(null, new LocalInstructionRunner(handlers.head, null, null, new TestSecretResolver(handler) {
      override def resolveSecrets(secrets: Seq[Secret]): Seq[ParameterValue] = {
        assert(secrets.size === 1)
        assert(secrets.head.name === "very")
        assert(secrets.head.path === "/secret/thingy")
        Seq(SimpleParameterValue("very", "cool"))
      }
    }, rugResolver = Some(resolver)))

    val result = Await.result(runner.run(handler.handle(null, SimpleParameterValues.Empty).get, None), 10.seconds)
    val results = result.log.collect { case i: InstructionResult => i }
    assert(results.head.instruction.detail.name === "HandleIt")
    assert(results.head.response.status === Status.Success)
    assert(results(1).instruction.detail.name === "ExampleFunction")
    assert(results(1).response.status === Status.Handled)
    assert(PlanResultInterpreter.interpret(result).status === Status.Success)
  }

  val instructionWithOnSuccessPlan = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "InstructionWithOnSuccessPlan.ts"))

  it("should run on success plan") {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(instructionWithOnSuccessPlan))
    val resolver = SimpleRugResolver(rugArchive)

    val handlers = resolver.resolvedDependencies.resolvedRugs
    val handler = handlers.collect { case i: CommandHandler => i }.head
    val runner = new LocalPlanRunner(null, new LocalInstructionRunner(handlers.head, null, null, new TestSecretResolver(handler) {
      override def resolveSecrets(secrets: Seq[Secret]): Seq[ParameterValue] = {
        assert(secrets.size === 1)
        assert(secrets.head.name === "very")
        assert(secrets.head.path === "/secret/thingy")
        Seq(SimpleParameterValue("very", "cool"))
      }
    }, rugResolver = Some(resolver)))

    val result = Await.result(runner.run(handler.handle(null, SimpleParameterValues.Empty).get, None), 10.seconds)
    val nestedPlanRun = result.log.head.asInstanceOf[NestedPlanRun]
    val nestedPlanResult = nestedPlanRun.planResult.value.get.get
    val nestedPlanInstructionResult = nestedPlanResult.log.head.asInstanceOf[InstructionResult]
    assert(nestedPlanInstructionResult.instruction.detail.name === "ExampleFunction")
    assert(nestedPlanInstructionResult.response.status === Status.Success)
    assert(nestedPlanInstructionResult.response.body === Some("nested success"))
  }
}

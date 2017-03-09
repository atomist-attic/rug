package com.atomist.rug.test.gherkin.handler.command

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig, RugArchiveReader}
import com.atomist.rug.TestUtils._
import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.rug.test.gherkin.{GherkinRunner, Passed}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class GherkinRunnerCommandHandlerTest extends FlatSpec with Matchers {

  import CommandHandlerTestTargets._

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  "command handler testing" should "verify no plan steps" in {
    val passingFeature1Steps =
      """
        |import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"
        |
        |Given("a sleepy country", f => {
        | console.log("Given invoked for handler")
        |})
        |When("a visionary leader enters", (rugContext, world) => {
        |   let handler = world.commandHandler("ReturnsEmptyPlanCommandHandler")
        |   world.invokeHandler(handler, {})
        |})
        |Then("excitement ensues", (p,world) => {
        |    return world.plan().messages().length == 0
        |})
      """.stripMargin
    val passingFeature1StepsFile = StringFileArtifact(
      ".atomist/test/handlers/command/PassingFeature1Step.ts",
      passingFeature1Steps
    )

    val handlerName = "ReturnsEmptyPlanCommandHandler.ts"
    val handlerFile = requiredFileInPackage(this, "CommandHandlers.ts").withPath(atomistConfig.handlersRoot + "/command/" + handlerName)
    val as = SimpleFileBasedArtifactSource(Feature1File, passingFeature1StepsFile, handlerFile)

    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Some(RugArchiveReader.find(cas)))
    val run = grt.execute()
    //println(new TestReport(run))
    run.result match {
      case Passed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "verify single returned message" in {
    val passingFeature1Steps =
      """
        |import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"
        |
        |Given("a sleepy country", f => {
        | console.log("Given invoked for handler")
        |})
        |When("a visionary leader enters", (rugContext, world) => {
        |   let handler = world.commandHandler("ReturnsOneMessageCommandHandler")
        |   world.invokeHandler(handler, {})
        |})
        |Then("excitement ensues", (p,world) => {
        |   console.log("The plan message were " + world.plan().messages())
        |    return world.plan().messages().length == 1
        |})
      """.stripMargin
    val passingFeature1StepsFile = StringFileArtifact(
      ".atomist/test/handlers/PassingFeature1Step.ts",
      passingFeature1Steps
    )

    val handlerName = "ReturnsOneMessageCommandHandler.ts"
    val handlerFile = requiredFileInPackage(this, "CommandHandlers.ts").withPath(atomistConfig.handlersRoot + "/command/" + handlerName)
    val as = SimpleFileBasedArtifactSource(Feature1File, passingFeature1StepsFile, handlerFile)

    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Some(RugArchiveReader.find(cas)))
    val run = grt.execute()
    //println(new TestReport(run))
    run.result match {
      case Passed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }
}

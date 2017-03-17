package com.atomist.rug.test.gherkin.handler.command

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig, RugArchiveReader}
import com.atomist.rug.TestUtils
import com.atomist.rug.TestUtils._
import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.rug.test.gherkin.handler.event.EventHandlerTestTargets
import com.atomist.rug.test.gherkin.{GherkinRunner, Passed}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource}
import org.scalatest.{FlatSpec, Matchers}

class GherkinRunnerCommandHandlerTest extends FlatSpec with Matchers {

  import CommandHandlerTestTargets._

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  val nodesFile: FileArtifact = requiredFileInPackage(EventHandlerTestTargets, "Nodes.ts",
    ".atomist/handlers/command")

  "command handler testing" should "verify no plan steps" in {
    val passingFeature1Steps =
      TestUtils.requiredFileInPackage(this, "NoMessagesFeature1Steps.ts")
          .withPath(".atomist/tests/handlers/command/NoMessagesFeature1Steps.ts")

    val handlerName = "ReturnsEmptyPlanCommandHandler.ts"
    val handlerFile = requiredFileInPackage(this, "CommandHandlers.ts").withPath(atomistConfig.handlersRoot + "/command/" + handlerName)
    val as = SimpleFileBasedArtifactSource(nodesFile, Feature1File, passingFeature1Steps, handlerFile)

    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Some(RugArchiveReader.find(cas)))
    val run = grt.execute()
    run.result match {
      case Passed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "verify single returned message" in {
    val passingFeature1Steps =
      TestUtils.requiredFileInPackage(this, "SingleMessageFeature1Steps.ts")
    val passingFeature1StepsFile = passingFeature1Steps.withPath(
      ".atomist/tests/handlers/command/PassingFeature1Step.ts")

    val handlerName = "ReturnsOneMessageCommandHandler.ts"
    val handlerFile = requiredFileInPackage(this, "CommandHandlers.ts").withPath(atomistConfig.handlersRoot + "/command/" + handlerName)
    val as = SimpleFileBasedArtifactSource(nodesFile, Feature1File, passingFeature1StepsFile, handlerFile)

    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Some(RugArchiveReader.find(cas)))
    val run = grt.execute()
    run.result match {
      case Passed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "verify execution of a path expression without match" in {
    val stepsFile = "SingleMessageFeature1StepsWithPathExpression.ts"
    val passingFeature1Steps =
      TestUtils.requiredFileInPackage(this, stepsFile)
    val passingFeature1StepsFile = passingFeature1Steps.withPath(
      s".atomist/tests/handlers/command/$stepsFile")

    val handlerName = "RunsPathExpressionCommandHandler.ts"
    val handlerFile = requiredFileInPackage(this, "CommandHandlers.ts").withPath(atomistConfig.handlersRoot + "/command/" + handlerName)
    val as = SimpleFileBasedArtifactSource(nodesFile, Feature1File, passingFeature1StepsFile, handlerFile)

    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Some(RugArchiveReader.find(cas)))
    val run = grt.execute()
    //println(new TestReport(run))
    run.result match {
      case Passed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "verify execution of a path expression with match" in {
    val stepsFile = "SingleMessageFeature1StepsWithMatchingPathExpression.ts"
    val passingFeature1Steps =
      TestUtils.requiredFileInPackage(this, stepsFile)
    val passingFeature1StepsFile = passingFeature1Steps.withPath(
      s".atomist/test/handlers/command/$stepsFile")

    val handlerName = "RunsMatchingPathExpressionCommandHandler.ts"
    val handlerFile = requiredFileInPackage(this, "CommandHandlers.ts").withPath(atomistConfig.handlersRoot + "/command/" + handlerName)
    val as = SimpleFileBasedArtifactSource(
      nodesFile,
      Feature1File,
      passingFeature1StepsFile, handlerFile)

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

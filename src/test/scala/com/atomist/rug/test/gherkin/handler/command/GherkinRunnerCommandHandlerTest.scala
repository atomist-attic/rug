package com.atomist.rug.test.gherkin.handler.command

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.TestUtils._
import com.atomist.rug.runtime.js.JavaScriptEngineContextFactory
import com.atomist.rug.test.gherkin.handler.event.EventHandlerTestTargets
import com.atomist.rug.test.gherkin.{Failed, GherkinRunner, Passed}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.rug.{RugArchiveReader, TestUtils}
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
    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)))
    val run = grt.execute()
    run.result match {
      case Passed =>
      case wtf =>
        wtf match {
          case Failed(msg, Some(t)) => t.printStackTrace()
          case _ =>
        }
        fail(s"Unexpected: $wtf")
    }
  }

  it should "fail appropriately when going off materialized graph" in {
    val passingFeature1Steps =
      TestUtils.requiredFileInPackage(this, "GoesOffGraphSteps.ts")
        .withPath(".atomist/tests/handlers/command/GoesOffGraphSteps.ts")

    val handlerFile = requiredFileInPackage(this, "CommandHandlers.ts").withPath(atomistConfig.handlersRoot + "/command/Handlers.ts")
    val as = SimpleFileBasedArtifactSource(nodesFile, Feature1File, passingFeature1Steps, handlerFile)

    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)))
    val run = grt.execute()
    run.result match {
      case Failed(_, _) =>
      case wtf =>
        fail(s"Unexpected: $wtf")
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
    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)))
    val run = grt.execute()
    run.result match {
      case Passed =>
      case wtf =>
        wtf match {
          case Failed(msg, Some(t)) => t.printStackTrace()
          case _ =>
        }
        fail(s"Unexpected: $wtf")
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
    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)))
    val run = grt.execute()
    //println(new TestReport(run))
    run.result match {
      case Passed =>
      case wtf =>
        wtf match {
          case Failed(msg, Some(t)) => t.printStackTrace()
          case _ =>
        }
        fail(s"Unexpected: $wtf")
    }
  }

  it should "verify execution of a path expression with match" in {
    val stepsFile = "SingleMessageFeature1StepsWithMatchingPathExpression.ts"
    val passingFeature1Steps =
      TestUtils.requiredFileInPackage(this, stepsFile)
    val passingFeature1StepsFile = passingFeature1Steps.withPath(
      s".atomist/tests/handlers/command/$stepsFile")

    val handlerName = "RunsMatchingPathExpressionCommandHandler.ts"
    val handlerFile = requiredFileInPackage(this, "CommandHandlers.ts").withPath(atomistConfig.handlersRoot + "/command/" + handlerName)
    val as = SimpleFileBasedArtifactSource(
      nodesFile,
      Feature1File,
      passingFeature1StepsFile, handlerFile)

    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)))
    val run = grt.execute()
    //println(new TestReport(run))
    run.result match {
      case Passed =>
      case wtf =>
        wtf match {
          case Failed(msg, Some(t)) => t.printStackTrace()
          case _ =>
        }
        fail(s"Unexpected: $wtf")
    }
  }

  it should "verify path expression drilling into projects" in
    drillIntoProject("LooksInProjectsSteps.ts")

  it should "verify path expression drilling into projects with cloning" in
    drillIntoProject("LooksInRealProjectsSteps.ts")

    private def drillIntoProject(stepsFile: String) {
    val passingFeature1Steps =
      TestUtils.requiredFileInPackage(this, stepsFile)
    val passingFeature1StepsFile = passingFeature1Steps.withPath(
      s".atomist/tests/handlers/command/$stepsFile")

    val handlerName = "LooksInProjects.ts"
    val handlerFile = requiredFileInPackage(this, "CommandHandlers.ts").withPath(atomistConfig.handlersRoot + "/command/" + handlerName)
    val as = SimpleFileBasedArtifactSource(
      nodesFile,
      Feature1File,
      passingFeature1StepsFile, handlerFile)

    val cas = TypeScriptBuilder.compileWithExtendedModel(as)
    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)))
    val run = grt.execute()
    run.result match {
      case Passed =>
      case wtf =>
        wtf match {
          case Failed(msg, (Some(t))) => t.printStackTrace()
          case _ =>
        }
        fail(s"Unexpected: $wtf")
    }
  }
}

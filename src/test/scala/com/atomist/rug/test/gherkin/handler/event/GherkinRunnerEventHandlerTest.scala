package com.atomist.rug.test.gherkin.handler.event

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.RugArchiveReader
import com.atomist.rug.TestUtils._
import com.atomist.rug.runtime.js.JavaScriptEngineContextFactory
import com.atomist.rug.test.gherkin.{Passed, _}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Files ending with "a," "b" etc are identical in effect from the point
  * of view of these tests
  */
class GherkinRunnerEventHandlerTest extends FlatSpec with Matchers {

  import EventHandlerTestTargets._

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  val nodesFile: FileArtifact = requiredFileInPackage(this, "Nodes.ts", ".atomist/handlers/event")

  "event handler testing" should "verify no plan steps without any path match" in {
    val passingFeature1StepsFile = requiredFileInPackage(
      this,
      "PassingFeature1Steps.ts",
      ".atomist/tests/handlers/event"
    )

    val handlerFile = requiredFileInPackage(this, "EventHandlers.ts", atomistConfig.handlersRoot + "/event")
    val as = SimpleFileBasedArtifactSource(Feature1File, passingFeature1StepsFile, handlerFile, nodesFile)

    //println(ArtifactSourceUtils.prettyListFiles(as))
    val cas = TypeScriptBuilder.compileWithModel(as)
    val msl = new MatchSavingListener

    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)), Seq(msl))
    val run = grt.execute()
    run.result match {
      case Passed =>
        assert(msl.matches.nonEmpty)
//        assert(msl.matches.exists(_.matched))
//        assert(msl.matches.exists(!_.matched))
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "produce informative message without a handler loaded" in {
    val passingFeature1StepsFile = requiredFileInPackage(
      this,
      "FailingFeature1Steps.ts",
      ".atomist/tests/handlers/event"
    )

    val handlerFile = requiredFileInPackage(this, "EventHandlers.ts", atomistConfig.handlersRoot + "/event")
    val as = SimpleFileBasedArtifactSource(Feature1File, passingFeature1StepsFile, handlerFile, nodesFile)

    //println(ArtifactSourceUtils.prettyListFiles(as))
    val cas = TypeScriptBuilder.compileWithModel(as)

    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)))
    val run = grt.execute()
    run.result match {
      case Failed(msg,_) =>
        assert(!msg.contains("null"))
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "produce good message when a test step fails with a void return and exception" in {
    val passingFeature1StepsFile = requiredFileInPackage(
      this,
      "FailsDueToStepError.ts",
      ".atomist/tests/handlers/event"
    )

    val handlerFile = requiredFileInPackage(this, "EventHandlers.ts", atomistConfig.handlersRoot + "/event")
    val as = SimpleFileBasedArtifactSource(Feature1File, passingFeature1StepsFile, handlerFile, nodesFile)

    //println(ArtifactSourceUtils.prettyListFiles(as))
    val cas = TypeScriptBuilder.compileWithModel(as)

    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)))
    val run = grt.execute()
    run.result match {
      case Failed(msg,_) =>
        assert(msg.contains("What in God's holy name are you blathering about"))
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "produce good message when a handler fails with an exception" in {
    val passingFeature1StepsFile = requiredFileInPackage(
      this,
      "FailsDueToHandlerError.ts",
      ".atomist/tests/handlers/event"
    )

    val handlerFile = requiredFileInPackage(this, "EventHandlers.ts", atomistConfig.handlersRoot + "/event")
    val as = SimpleFileBasedArtifactSource(Feature1File, passingFeature1StepsFile, handlerFile, nodesFile)

    //println(ArtifactSourceUtils.prettyListFiles(as))
    val cas = TypeScriptBuilder.compileWithModel(as)

    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)))
    val run = grt.execute()
    run.result match {
      case Failed(msg,_) =>
        assert(msg.contains("hate"))
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "use generated model"
    useGeneratedModel("PassingFeature1StepsAgainstGenerated.ts")

  it should "use generated model and run path expression"
    useGeneratedModel("PassingFeature1StepsAgainstGeneratedWithPathExpression.ts")

  it should "use generated model with enum/narrowed type"
    useGeneratedModel("PassingFeature1StepsAgainstGenerated4.ts")

  it should "#487: use generated model with arrays" in
    useGeneratedModel("PassingFeature1StepsAgainstGeneratedWithArrays.ts")

  private def useGeneratedModel(stepsFile: String): Unit = {
    val passingFeature1StepsFile = requiredFileInPackage(
      this,
      stepsFile,
      ".atomist/tests/handlers/event"
    )

    val handlerFile = requiredFileInPackage(this, "EventHandlersAgainstGenerated.ts", atomistConfig.handlersRoot + "/event")
    val as = SimpleFileBasedArtifactSource(Feature1File, passingFeature1StepsFile, handlerFile)

    val cas = TypeScriptBuilder.compileWithExtendedModel(as)
    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)))
    val run = grt.execute()
    run.result match {
      case Passed =>
      case wtf: Failed =>
        if(wtf.cause.nonEmpty){
          wtf.cause.get.printStackTrace()
        }
        fail(s"Unexpected: $wtf")
      case _ =>
    }
  }

  it should "verify no plan steps with matching simple path match" in
    verifyNoPlanStepsWithMatchingSimplePathMatch("PassingFeature1Steps2.ts")

  it should "verify no plan steps with matching simple path match using named type" in
    verifyNoPlanStepsWithMatchingSimplePathMatch("PassingFeature1Steps2a.ts")

  it should "verify no plan steps with matching simple path match with void return" in
    verifyNoPlanStepsWithMatchingSimplePathMatch("PassingFeature1Steps2d.ts")

  it should "verify no plan steps with matching deeper path match" in
    verifyNoPlanStepsWithMatchingSimplePathMatch("PassingFeature1Steps4.ts")

  it should "#480 verify no plan steps with more than one handler registered" in
    verifyNoPlanStepsWithMatchingSimplePathMatch("PassingFeature1Steps5.ts")

  def verifyNoPlanStepsWithMatchingSimplePathMatch(stepsFile: String) {
    val passingFeature1StepsFile = requiredFileInPackage(
      this,
      stepsFile,
      ".atomist/tests/handler/event"
    )
    val handlerFile = requiredFileInPackage(this, "EventHandlers.ts", atomistConfig.handlersRoot + "/event")
    val as = SimpleFileBasedArtifactSource(Feature1File, passingFeature1StepsFile, handlerFile, nodesFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val msl = new MatchSavingListener
    val grt = new GherkinRunner(
      JavaScriptEngineContextFactory.create(cas),
      Some(RugArchiveReader(cas)),
      listeners = Seq(msl))
    val run = grt.execute()
    assert(msl.matches.nonEmpty)
    assert(msl.matches.exists(_.matched))
    run.result match {
      case Passed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "verify no plan steps with non-matching simple path match" in {
    val passingFeature1StepsFile = requiredFileInPackage(
      this,
      "PassingFeature1Steps3.ts",
      ".atomist/tests/handler/event"
    )

    val handlerFile = requiredFileInPackage(this, "EventHandlers.ts", atomistConfig.handlersRoot + "/event")
    val as = SimpleFileBasedArtifactSource(Feature1File, passingFeature1StepsFile, handlerFile, nodesFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)))
    val run = grt.execute()
    run.result match {
      case Passed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "return the message from the handler when no plan was explicitly returned and more than one event fired" in
    feature2Steps("PassingFeature2Steps1.ts")

  private def feature2Steps(stepsFile: String): Unit = {
    val passingFeature1StepsFile = requiredFileInPackage(
      this,
      stepsFile,
      ".atomist/tests/handler/event"
    )

    val handlerFile = requiredFileInPackage(this, "EventHandlers.ts", atomistConfig.handlersRoot + "/event")
    val as = SimpleFileBasedArtifactSource(Feature2File, passingFeature1StepsFile, handlerFile, nodesFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)))
    val run = grt.execute()
    run.result match {
      case Passed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "#517: Not encounter not a GraphNode" in {
    val steps517 = requiredFileInPackage(this, "517_steps.ts", ".atomist/tests/handlers/event")
    val feature517 = requiredFileInPackage(this, "517.feature", ".atomist/tests/handlers/event")
    val handlerFile = requiredFileInPackage(this, "517_Pulled.ts", ".atomist/handlers/event")
    val as = SimpleFileBasedArtifactSource(feature517, handlerFile, steps517)

    val cas = TypeScriptBuilder.compileWithExtendedModel(as)

    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)))
    val run = grt.execute()
    run.result match {
      case Passed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "navigate into Project from Push" in {
    val steps517 = requiredFileInPackage(this, "PushProjectSteps.ts", ".atomist/tests/handlers/event")
    val feature517 = requiredFileInPackage(this, "PushProject.feature", ".atomist/tests/handlers/event")
    val handlerFile = requiredFileInPackage(this, "PushProject.ts", ".atomist/handlers/event")
    val as = SimpleFileBasedArtifactSource(feature517, handlerFile, steps517)

    val cas = TypeScriptBuilder.compileWithExtendedModel(as)

    val grt = new GherkinRunner(JavaScriptEngineContextFactory.create(cas), Some(RugArchiveReader(cas)))
    val run = grt.execute()
    run.result match {
      case Passed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }
}

private class MatchSavingListener extends GherkinExecutionListenerAdapter {

  var matches: Seq[PathExpressionEvaluation] = Nil

  override def pathExpressionResult(peval: PathExpressionEvaluation): Unit = {
    matches = matches :+ peval
  }
}

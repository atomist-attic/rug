package com.atomist.rug.test.gherkin.handler.event

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig, RugArchiveReader}
import com.atomist.rug.TestUtils._
import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.rug.test.gherkin.{GherkinRunner, Passed}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{ArtifactSourceUtils, FileArtifact, SimpleFileBasedArtifactSource}
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

    val grt = new GherkinRunner(new JavaScriptContext(cas), Some(RugArchiveReader.find(cas)))
    val run = grt.execute()
    run.result match {
      case Passed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "verify no plan steps with matching simple path match" in
    verifyNoPlanStepsWithMatchingSimplePathMatch("PassingFeature1Steps2.ts")

  it should "verify no plan steps with matching simple path match using named type" in
    verifyNoPlanStepsWithMatchingSimplePathMatch("PassingFeature1Steps2a.ts")

  it should "verify no plan steps with matching simple path match using named type and root query by example" in
    verifyNoPlanStepsWithMatchingSimplePathMatch("PassingFeature1Steps2b.ts")

  it should "verify no plan steps with matching simple path match using named type and leaf query by example" in
    verifyNoPlanStepsWithMatchingSimplePathMatch("PassingFeature1Steps2c.ts")

  it should "verify no plan steps with matching deeper path match" in
    verifyNoPlanStepsWithMatchingSimplePathMatch("PassingFeature1Steps4.ts")

  def verifyNoPlanStepsWithMatchingSimplePathMatch(stepsFile: String) {
    val passingFeature1StepsFile = requiredFileInPackage(
      this,
      stepsFile,
      ".atomist/tests/handler/event"
    )
    val handlerFile = requiredFileInPackage(this, "EventHandlers.ts", atomistConfig.handlersRoot + "/event")
    val as = SimpleFileBasedArtifactSource(Feature1File, passingFeature1StepsFile, handlerFile, nodesFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Some(RugArchiveReader.find(cas)))
    val run = grt.execute()
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
    val grt = new GherkinRunner(new JavaScriptContext(cas), Some(RugArchiveReader.find(cas)))
    val run = grt.execute()
    run.result match {
      case Passed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

}

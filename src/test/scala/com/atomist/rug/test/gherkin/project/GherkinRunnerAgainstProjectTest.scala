package com.atomist.rug.test.gherkin.project

import com.atomist.parse.java.ParsingTargets
import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.rug.test.gherkin._
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.rug.{RugArchiveReader, TestUtils}
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import gherkin.ast.ScenarioDefinition
import org.scalatest.{FlatSpec, Matchers}

class GherkinRunnerAgainstProjectTest extends FlatSpec with Matchers {

  private lazy val alpEditorsFile = TestUtils.requiredFileInPackage(this, "AlpEditors.ts").withPath(".atomist/editors/AlpEditors.ts")

  import GherkinReaderTest._
  import ProjectTestTargets._

  "Gherkin project testing" should "fail without JS" in {
    val el = new TestExecutionListener
    val as = SimpleFileBasedArtifactSource(TwoScenarioFeatureFile)
    val grt = new GherkinRunner(new JavaScriptContext(as), None, Seq(el))
    val run = grt.execute()
    assert(run.result.isInstanceOf[NotYetImplemented])
    assert(el.fsCount == 1)
    assert(el.fcCount == 1)
    assert(el.ssCount == 2)
    assert(el.scCount == 2)
  }

  it should "pass with passing JS" in {
    val as = SimpleFileBasedArtifactSource(SimpleFeatureFile, PassingSimpleTsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas))
    assert(grt.execute().result === Passed)
  }

  it should "fail with failing JS" in {
    val as = SimpleFileBasedArtifactSource(SimpleFeatureFile, FailingSimpleTsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas))
    val run = grt.execute()
    run.result match {
      case _: Failed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "fail if Given step is not implemented" in {
    val as = SimpleFileBasedArtifactSource(NotImplementedGivenFeatureFile, PassingSimpleTsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas))
    val run = grt.execute()
    assert(run.result.isInstanceOf[NotYetImplemented])
  }

  it should "fail if When step is not implemented" in {
    val as = SimpleFileBasedArtifactSource(NotImplementedWhenFeatureFile, PassingSimpleTsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas))
    val run = grt.execute()
    assert(run.result.isInstanceOf[NotYetImplemented])
  }

  it should "run an editor without parameters" in {
    val as = SimpleFileBasedArtifactSource(
      alpEditorsFile,
      SimpleFeatureFile,
      EditorWithoutParametersTsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Option(RugArchiveReader(cas)))
    val run = grt.execute()
    assert(run.result === Passed)
  }

  it should "run an editor with parameters" in {
    val el = new TestExecutionListener
    val as =
      SimpleFileBasedArtifactSource(
        alpEditorsFile,
        SimpleFeatureFile,
        EditorWithParametersStepsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Option(RugArchiveReader(cas)), Seq(el))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result === Passed)
    assert(el.fsCount == 1)
    assert(el.fcCount == 1)
    assert(el.ssCount == 1)
    assert(el.scCount == 1)
    assert(el.fResult.passed)
    assert(el.sResult.passed)
  }

  it should "not run a feature based on filter" in {
    val el = new TestExecutionListener
    val as =
      SimpleFileBasedArtifactSource(
        alpEditorsFile,
        SimpleFeatureFile,
        EditorWithParametersStepsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Option(RugArchiveReader(cas)), Seq(el))
    val run = grt.execute((fd: FeatureDefinition) => {!fd.feature.getName.equals("Australian political history")})
    assert(run.testCount == 0)
    assert(run.result === Passed)
    assert(el.fsCount == 0)
    assert(el.fcCount == 0)
    assert(el.ssCount == 0)
    assert(el.scCount == 0)
  }

  it should "handle an editor test checking for an invalid parameter" in {
    val el = new TestExecutionListener
    val as = SimpleFileBasedArtifactSource(
      alpEditorsFile,
      EditorBadParameterFeatureFile,
      EditorWithBadParametersStepsFile
    )
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Option(RugArchiveReader(cas)), Seq(el))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result === Passed)
  }

  it should "support parameterized steps" in {
    val el = new TestExecutionListener
    val as = SimpleFileBasedArtifactSource(
      ParameterizedFeatureFile,
      StringFileArtifact(
        ".atomist/tests/project/ParameterizedFeatureSteps.ts",
        TestUtils.contentOf(this, "ParameterizedFeatureSteps.ts"))
    )
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Option(RugArchiveReader(cas)), Seq(el))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result === Passed)
  }

  it should "test a generator that copies starting content without parameters" in {
    val el = new TestExecutionListener
    val projectName = "generator-test"
    val atomistStuff: ArtifactSource =
      TestUtils.resourcesInPackage(this).filter(_ => true, f => f.name == "SimpleGenerator.ts")
        .withPathAbove(".atomist/editors") +
        SimpleFileBasedArtifactSource(
          GenerationFeatureFile,
          StringFileArtifact(".atomist/tests/project/GenerationSteps.ts",
            generationTest("SimpleGenerator", projectName, Map()))
        )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive), Option(RugArchiveReader(rugArchive)), Seq(el))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result === Passed)
    assert(el.fsCount == 1)
    assert(el.fcCount == 1)
    assert(el.ssCount == 1)
    assert(el.scCount == 1)
    assert(el.fResult.passed)
    assert(el.sResult.passed)
  }

  it should "test a generator that copies starting content with parameters" in {
    val projectName = "generator-params-test"
    val atomistStuff: ArtifactSource =
      TestUtils.resourcesInPackage(this).filter(_ => true, f => f.name == "SimpleGeneratorWithParams.ts")
        .withPathAbove(".atomist/editors") +
        SimpleFileBasedArtifactSource(
          GenerationFeatureFile,
          StringFileArtifact(".atomist/tests/project/GenerationSteps.ts",
            generationTest("SimpleGeneratorWithParams", projectName, Map("text" -> "`Anders Hjelsberg is God`")))
        )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive), Some(RugArchiveReader(rugArchive)))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result === Passed)
  }

  it should "test passing generator invalid parameters" in {
    val projectName = "generator-invalid-params-test"
    val atomistStuff: ArtifactSource =
      TestUtils.resourcesInPackage(this).filter(_ => true, f => f.name == "SimpleGeneratorWithParams.ts")
        .withPathAbove(".atomist/editors") +
        SimpleFileBasedArtifactSource(
          GenerationBadParameterFeatureFile,
          StringFileArtifact(".atomist/tests/project/GenerationSteps.ts",
            generateWithInvalidParameters("SimpleGeneratorWithParams", projectName, Map("text" -> "`Anders Hjelsberg is 1God`")))
        )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive), Some(RugArchiveReader(rugArchive)))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result === Passed)
  }

  /**
    * This generator deliberately fails. We want to see a good error message.
    */
  it should "test a generator failing with deliberate exception" in {
    val projectName = "generator-fail-test"
    val atomistStuff: ArtifactSource =
        SimpleFileBasedArtifactSource(
          TestUtils.requiredFileInPackage(this, "FailingGenerator.ts").withPath(".atomist/generators/FailingGenerator.ts"),
          GenerationFeatureFile,
          StringFileArtifact(".atomist/tests/project/GenerationSteps.ts",
            generationTest("FailingGenerator", projectName, Map()))
        )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive), Some(RugArchiveReader(rugArchive)))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result.isInstanceOf[Failed])
    assert(run.result.message.contains("I hate the world"))
  }

  it should "successfully test for an aborted scenario" in {
    val projectName = "generator-fail-test"
    val atomistStuff: ArtifactSource =
      SimpleFileBasedArtifactSource(
        TestUtils.requiredFileInPackage(this, "FailingGenerator.ts").withPath(".atomist/generators/FailingGenerator.ts"),
        GenerationFailureFeatureFile,
        StringFileArtifact(".atomist/tests/project/GenerationSteps.ts",
          generationTest("FailingGenerator", projectName, Map()))
      )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive), Some(RugArchiveReader(rugArchive)))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result === Passed)
  }

  it should "fail when testing for an aborted scenario that does not" in {
    val projectName = "generator-non-abort"
    val atomistStuff: ArtifactSource =
      SimpleFileBasedArtifactSource(
        TestUtils.requiredFileInPackage(this, "SimpleGenerator.ts").withPath(".atomist/generators/SimpleGenerator.ts"),
        GenerationFailureFeatureFile,
        StringFileArtifact(".atomist/tests/project/GenerationSteps.ts",
          generationTest("SimpleGenerator", projectName, Map()))
      )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive), Some(RugArchiveReader(rugArchive)))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result.isInstanceOf[Failed])
    assert(run.result.message.contains("aborted"))
  }

  it should "access a real project" in
    edit("real_edit.feature")

  it should "access a branch of a real project" in
    edit("real_edit_with_branch.feature")

  private def edit(feature: String): Unit = {
    val el = new TestExecutionListener
    val as = SimpleFileBasedArtifactSource(
      StringFileArtifact(
        ".atomist/tests/project/RealEditSteps.ts",
        TestUtils.contentOf(this, "RealEditSteps.ts")),
      StringFileArtifact(
        ".atomist/tests/project/real_edit.feature",
        TestUtils.contentOf(this, feature))
    )
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Option(RugArchiveReader(cas)), Seq(el))
    val run = grt.execute()
    assert(run.testCount > 0)
    run.result match {
      case Failed(msg, Some(cause)) => cause.printStackTrace()
      case _ =>
    }
    assert(run.result === Passed)
  }

  class TestExecutionListener extends GherkinExecutionListenerAdapter {

    var fsCount = 0
    var fcCount = 0
    var ssCount = 0
    var scCount = 0
    var fResult: FeatureResult = _
    var sResult: ScenarioResult = _

    override def featureStarting(feature: FeatureDefinition): Unit = {
      fsCount = fsCount + 1
    }

    override def featureCompleted(feature: FeatureDefinition, result: FeatureResult): Unit = {
      fcCount = fcCount + 1
      fResult = result
    }

    override def scenarioStarting(scenario: ScenarioDefinition): Unit = {
      ssCount = ssCount + 1
    }

    override def scenarioCompleted(scenario: ScenarioDefinition, result: ScenarioResult): Unit = {
      scCount = scCount + 1
      sResult = result
    }
  }

}

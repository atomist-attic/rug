package com.atomist.rug.test.gherkin.project

import javax.script.ScriptContext

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.archive.RugArchiveReader
import com.atomist.rug.TestUtils
import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.rug.test.gherkin._
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import gherkin.ast.ScenarioDefinition
import jdk.nashorn.api.scripting.{NashornScriptEngine, NashornScriptEngineFactory}
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

  it should "run an editor without parameters" in {
    val as = SimpleFileBasedArtifactSource(
      alpEditorsFile,
      SimpleFeatureFile,
      EditorWithoutParametersTsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Option(RugArchiveReader.find(cas)))
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
    val grt = new GherkinRunner(new JavaScriptContext(cas), Option(RugArchiveReader.find(cas)), Seq(el))
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
    val grt = new GherkinRunner(new JavaScriptContext(cas), Option(RugArchiveReader.find(cas)), Seq(el))
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
    val grt = new GherkinRunner(new JavaScriptContext(cas), Option(RugArchiveReader.find(cas)), Seq(el))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result === Passed)
  }

  it should "test a reviewer" in {
    val as =
      SimpleFileBasedArtifactSource(
        TestUtils.requiredFileInPackage(this, "FindCorruption.ts").withPath(".atomist/editors/FindCorruption.ts"),
        CorruptionFeatureFile,
        StringFileArtifact(".atomist/tests/project/CorruptionTestSteps.ts", TestUtils.requiredFileInPackage(this, "CorruptionTestSteps.ts").content)
      )
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result === Passed)
    val sum = new TestReport(run).testSummary
    assert(sum.contains("SUCCESS"))
  }

  it should "test a generator that copies starting content without parameters" in {
    val el = new TestExecutionListener
    val projectName = "generator-test"
    val atomistStuff: ArtifactSource =
      TestUtils.resourcesInPackage(this).filter(_ => true, f => f.name == "SimpleGenerator.ts")
        .withPathAbove(".atomist/editors") +
        SimpleFileBasedArtifactSource(
          GenerationFeatureFile,
          StringFileArtifact(".atomist/tests/project/GenerationSteps.ts", generationTest("SimpleGenerator", projectName, Map()))
        )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive), Option(RugArchiveReader.find(rugArchive)), Seq(el))
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
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive), Some(RugArchiveReader.find(rugArchive)))
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
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive), Some(RugArchiveReader.find(rugArchive)))
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
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result.isInstanceOf[Failed])
  }

  it should "run two sets of tests without side effect" in {
      val as = SimpleFileBasedArtifactSource(
        TestUtils.requiredFileInPackage(this, "FindCorruption.ts").withPath(".atomist/editors/FindCorruption.ts"),
        CorruptionFeatureFile,
        StringFileArtifact(".atomist/tests/project/CorruptionTestSteps.ts", TestUtils.requiredFileInPackage(this, "CorruptionTestSteps.ts").content)
      )
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt1 = new GherkinRunner(new JavaScriptContext(cas))
    val run1 = grt1.execute()
    val grt2 = new GherkinRunner(new JavaScriptContext(cas))
    val run2 = grt2.execute()

    val hopefullyCleanEngine = new NashornScriptEngineFactory()
      .getScriptEngine("--optimistic-types", "--language=es6", "--no-java")
      .asInstanceOf[NashornScriptEngine]
    import GherkinRunner._
    hopefullyCleanEngine.getBindings(ScriptContext.ENGINE_SCOPE).containsKey(DefinitionsObjectName) should be (false)
    val globs = hopefullyCleanEngine.getBindings(ScriptContext.GLOBAL_SCOPE)
    (globs == null || !globs.containsKey(DefinitionsObjectName)) should be (true)
  }

  class TestExecutionListener extends GherkinExecutionListenerAdapter {

    var fsCount = 0
    var fcCount = 0
    var ssCount = 0
    var scCount = 0
    var fResult: FeatureResult = null
    var sResult: ScenarioResult = null

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

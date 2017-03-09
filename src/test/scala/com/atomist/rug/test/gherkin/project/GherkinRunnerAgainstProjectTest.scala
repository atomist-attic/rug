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

  import GherkinReaderTest._
  import ProjectTestTargets._

  "Gherkin project testing" should "fail without JS" in {
    val el = new TestExecutionListener
    val as = SimpleFileBasedArtifactSource(TwoScenarioFeatureFile)
    val grt = new GherkinRunner(new JavaScriptContext(as), None, Seq(el))
    val run = grt.execute()
    assert(run.result.isInstanceOf[NotYetImplemented])
    println(new TestReport(run).testSummary)
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
    println(new TestReport(run))
    run.result match {
      case _: Failed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "run an editor without parameters" in {
    val alpEditor =
      """
        |import {Project} from '@atomist/rug/model/Core'
        |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
        |
        |export class AlpEditor implements ProjectEditor {
        |    name: string = "AlpEditor"
        |    description: string = "ALP history"
        |
        |    edit(project: Project) {
        |     project.addFile("Paul", "Can a souffle rise twice?")
        |    }
        |}
        |
        |export let xx_editor = new AlpEditor()
      """.stripMargin
    val as = SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/AlpEditor.ts", alpEditor),
      SimpleFeatureFile,
      EditorWithoutParametersTsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Option(RugArchiveReader.find(cas)))
    val run = grt.execute()
    assert(run.result === Passed)
    println(new TestReport(run).testSummary)
  }

  it should "run an editor with parameters" in {
    val el = new TestExecutionListener
    val as = TestUtils.resourcesInPackage(this).withPathAbove(".atomist/editors") +
      SimpleFileBasedArtifactSource(
        SimpleFeatureFile,
        EditorWithParametersTsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas), Option(RugArchiveReader.find(cas)), Seq(el))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result === Passed)
    //println(new TestReport(run).testSummary)
    assert(el.fsCount == 1)
    assert(el.fcCount == 1)
    assert(el.ssCount == 1)
    assert(el.scCount == 1)
    assert(el.fResult.passed)
    assert(el.sResult.passed)
  }

  it should "test a reviewer" in {
    val as = TestUtils.resourcesInPackage(this).withPathAbove(".atomist/editors") +
      SimpleFileBasedArtifactSource(
        CorruptionFeatureFile,
        StringFileArtifact(".atomist/test/project/CorruptionSteps.ts", CorruptionTest)
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
    val atomistStuff: ArtifactSource =
      TestUtils.resourcesInPackage(this).filter(_ => true, f => f.name == "SimpleGenerator.ts")
        .withPathAbove(".atomist/editors") +
        SimpleFileBasedArtifactSource(
          GenerationFeatureFile,
          StringFileArtifact(".atomist/test/project/GenerationSteps.ts", generationTest("SimpleGenerator", Map()))
        )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    //println(ArtifactSourceUtils.prettyListFiles(rugArchive))
    //println(rugArchive.findFile(".atomist/test/GenerationSteps.js").get.content)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive), Option(RugArchiveReader.find(rugArchive)), Seq(el))
    val run = grt.execute()
    assert(run.testCount > 0)
    //println(run.result)
    assert(run.result === Passed)
    assert(el.fsCount == 1)
    assert(el.fcCount == 1)
    assert(el.ssCount == 1)
    assert(el.scCount == 1)
    assert(el.fResult.passed)
    assert(el.sResult.passed)
  }

  it should "test a generator that copies starting content with parameters" in {
    val atomistStuff: ArtifactSource =
      TestUtils.resourcesInPackage(this).filter(_ => true, f => f.name == "SimpleGeneratorWithParams.ts")
        .withPathAbove(".atomist/editors") +
        SimpleFileBasedArtifactSource(
          GenerationFeatureFile,
          StringFileArtifact(".atomist/test/project/GenerationSteps.ts",
            generationTest("SimpleGeneratorWithParams", Map("text" -> "`Anders Hjelsberg is God`")))
        )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive), Some(RugArchiveReader.find(rugArchive)))
    val run = grt.execute()
    assert(run.testCount > 0)
    //println(run.result)
    assert(run.result === Passed)
  }

  it should "test passing generator invalid parameters" in {
    val atomistStuff: ArtifactSource =
      TestUtils.resourcesInPackage(this).filter(_ => true, f => f.name == "SimpleGeneratorWithParams.ts")
        .withPathAbove(".atomist/editors") +
        SimpleFileBasedArtifactSource(StringFileArtifact(
          ".atomist/test/project/Simple.feature",
          """
            |Feature: Generate a new project
            | This is a test
            | to see whether
            | we can test project generators
            |
            |Scenario: New project should have content from template
            | Given an empty project
            | When run simple generator
            | Then parameters were invalid
          """.stripMargin),
          StringFileArtifact(".atomist/test/project/GenerationSteps.ts",
            generateWithInvalidParameters("SimpleGeneratorWithParams", Map("text" -> "`Anders Hjelsberg is 1God`")))
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
    val atomistStuff: ArtifactSource =
      TestUtils.resourcesInPackage(this).filter(_ => true, f => f.name.contains("Failing"))
        .withPathAbove(".atomist/generators") +
        SimpleFileBasedArtifactSource(
          GenerationFeatureFile,
          StringFileArtifact(".atomist/test/project/GenerationSteps.ts", generationTest("FailingGenerator", Map()))
        )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result.isInstanceOf[Failed])
  }

  it should "run two sets of tests without side effect" in {
    val as = TestUtils.resourcesInPackage(this).withPathAbove(".atomist/editors") +
      SimpleFileBasedArtifactSource(
        CorruptionFeatureFile,
        StringFileArtifact(".atomist/test/project/CorruptionSteps.ts", CorruptionTest)
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

  class TestExecutionListener extends GherkinExecutionListener {

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

package com.atomist.rug.test.gherkin

import javax.script.ScriptContext

import com.atomist.parse.java.ParsingTargets
import com.atomist.rug.TestUtils
import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{ArtifactSource, ArtifactSourceUtils, SimpleFileBasedArtifactSource, StringFileArtifact}
import jdk.nashorn.api.scripting.{NashornScriptEngine, NashornScriptEngineFactory}
import org.scalatest.{FlatSpec, Matchers}

class GherkinRunnerTest extends FlatSpec with Matchers {

  import GherkinReaderTest._

  "Gherkin runner" should "fail without JS" in {
    val as = SimpleFileBasedArtifactSource(TwoScenarioFeatureFile)
    val grt = new GherkinRunner(new JavaScriptContext(as))
    val run = grt.execute()
    assert(run.result.isInstanceOf[NotYetImplemented])
    println(new TestReport(run).testSummary)
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
        |let xx_editor = new AlpEditor()
      """.stripMargin
    val as = SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/AlpEditor.ts", alpEditor),
      SimpleFeatureFile,
      EditorWithoutParametersTsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas))
    val run = grt.execute()
    assert(run.result === Passed)
    println(new TestReport(run).testSummary)
  }

  it should "run an editor with parameters" in {
    val as = TestUtils.resourcesInPackage(this).withPathAbove(".atomist/editors") +
      SimpleFileBasedArtifactSource(
        SimpleFeatureFile,
        EditorWithParametersTsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas))
    val run = grt.execute()
    assert(run.testCount > 0)
    assert(run.result === Passed)
    //println(new TestReport(run).testSummary)
  }

  it should "test a reviewer" in {
    val as = TestUtils.resourcesInPackage(this).withPathAbove(".atomist/editors") +
      SimpleFileBasedArtifactSource(
        CorruptionFeatureFile,
        StringFileArtifact(".atomist/test/CorruptionSteps.ts", CorruptionTest)
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
    val atomistStuff: ArtifactSource =
      TestUtils.resourcesInPackage(this).filter(_ => true, f => f.name == "SimpleGenerator.ts")
        .withPathAbove(".atomist/generators") +
        SimpleFileBasedArtifactSource(
          GenerationFeatureFile,
          StringFileArtifact(".atomist/test/GenerationSteps.ts", generationTest("SimpleGenerator", Map()))
        )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    //println(ArtifactSourceUtils.prettyListFiles(rugArchive))
    //println(rugArchive.findFile(".atomist/test/GenerationSteps.js").get.content)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive))
    val run = grt.execute()
    assert(run.testCount > 0)
    //println(run.result)
    assert(run.result === Passed)
  }

  it should "test a generator that copies starting content with parameters" in {
    val atomistStuff: ArtifactSource =
      TestUtils.resourcesInPackage(this).filter(_ => true, f => f.name == "SimpleGeneratorWithParams.ts")
        .withPathAbove(".atomist/generators") +
        SimpleFileBasedArtifactSource(
          GenerationFeatureFile,
          StringFileArtifact(".atomist/test/GenerationSteps.ts",
            generationTest("SimpleGeneratorWithParams", Map("text" -> "`Anders Hjelsberg is God`")))
        )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive))
    val run = grt.execute()
    assert(run.testCount > 0)
    //println(run.result)
    assert(run.result === Passed)
  }

  it should "test giving a generator invalid parameters" in pendingUntilFixed {
    val atomistStuff: ArtifactSource =
      TestUtils.resourcesInPackage(this).filter(_ => true, f => f.name == "SimpleGeneratorWithParams.ts")
        .withPathAbove(".atomist/generators") +
        SimpleFileBasedArtifactSource(
          GenerationFeatureFile,
          StringFileArtifact(".atomist/test/GenerationSteps.ts",
            // Fails due to numbers
            generationTest("SimpleGeneratorWithParams", Map("text" -> "`Anders Hjelsberg is 1 God`")))
        )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive))
    val run = grt.execute()
    assert(run.testCount > 0)
    println(run.result)
    assert(run.result.isInstanceOf[Failed])
    assert(!run.featureResults.exists(fr => fr.assertions.exists(_.passed)))
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
          StringFileArtifact(".atomist/test/GenerationSteps.ts", generationTest("FailingGenerator", Map()))
        )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    //println(rugArchive.findFile(".atomist/test/GenerationSteps.js").get.content)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive))
    val run = grt.execute()
    assert(run.testCount > 0)
    //println(run.result)
    assert(run.result.isInstanceOf[Failed])
  }

  it should "run two sets of tests without side effect" in {
    val as = TestUtils.resourcesInPackage(this).withPathAbove(".atomist/editors") +
      SimpleFileBasedArtifactSource(
        CorruptionFeatureFile,
        StringFileArtifact(".atomist/test/CorruptionSteps.ts", CorruptionTest)
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

}

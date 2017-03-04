package com.atomist.rug.test.gherkin

import com.atomist.parse.java.ParsingTargets
import com.atomist.rug.TestUtils
import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{ArtifactSource, ArtifactSourceUtils, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class GherkinRunnerTest extends FlatSpec with Matchers {

  import GherkinReaderTest._

  it should "fail without JS" in {
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
      case f: Failed =>
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
    println(new TestReport(run).testSummary)
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
    println(new TestReport(run).testSummary)
  }

  it should "test a generator" in {
    val atomistStuff: ArtifactSource =
      TestUtils.resourcesInPackage(this).filter(_ => true, f => f.name.contains("Gen"))
        .withPathAbove(".atomist/generators") +
        SimpleFileBasedArtifactSource(
          GenerationFeatureFile,
          StringFileArtifact(".atomist/test/GenerationSteps.ts", GenerationTest)
        )

    val projTemplate = ParsingTargets.NewStartSpringIoProject
    val rugArchive = TypeScriptBuilder.compileWithModel(atomistStuff + projTemplate)
    //println(ArtifactSourceUtils.prettyListFiles(rugArchive))
    //println(rugArchive.findFile(".atomist/test/GenerationSteps.js").get.content)
    val grt = new GherkinRunner(new JavaScriptContext(rugArchive))
    val run = grt.execute()
    assert(run.testCount > 0)
    println(run.result)
    assert(run.result === Passed)
    println(new TestReport(run).testSummary)
  }

}

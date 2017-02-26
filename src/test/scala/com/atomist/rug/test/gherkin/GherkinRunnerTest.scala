package com.atomist.rug.test.gherkin

import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class GherkinRunnerTest extends FlatSpec with Matchers {

  import GherkinReaderTest._

  it should "fail without JS" in {
    val as = SimpleFileBasedArtifactSource(TwoScenarioFile)
    val grt = new GherkinRunner(new JavaScriptContext(as))
    assert(grt.execute().result === NotYetImplemented)
  }

  it should "pass with passing JS" in {
    val as = SimpleFileBasedArtifactSource(SimpleFile, PassingSimpleTsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas))
    assert(grt.execute().result === Passed)
  }

  it should "fail with failing JS" in {
    val as = SimpleFileBasedArtifactSource(SimpleFile, FailingSimpleTsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas))
    grt.execute().result match {
      case f: Failed =>
        println(f)
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  /* PASSING BUT NOT ACTUALLY RUNNING
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
        |    newImport = "import org.scalatest.DiagrammedAssertions._"
        |
        |    edit(project: Project) {
        |     project.addFile("Paul", "Can a souffle rise twice?")
        |    }
        |}
        |
        |let xx_editor = new AlpEditor()
      """.stripMargin
    val as = SimpleFileBasedArtifactSource(StringFileArtifact(".atomist/editors/AlpEditor.ts", alpEditor), EditorSimpleTsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas))
    assert(grt.execute().result === Passed)
  }
  */

}

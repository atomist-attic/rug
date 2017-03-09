package com.atomist.rug.test.gherkin.handler

import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.rug.test.gherkin.GherkinReaderTest.SimpleFeatureFile
import com.atomist.rug.test.gherkin.{Failed, GherkinRunner, TestReport}
import com.atomist.rug.test.gherkin.project.ProjectTestTargets.FailingSimpleTsFile
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.SimpleFileBasedArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class GherkinRunnerAgainstHandlersTest extends FlatSpec with Matchers {

  import HandlerTestTargets._

  "handler testing" should "perform simple steps" in {
    val as = SimpleFileBasedArtifactSource(Feature1File, PassingFeature1StepsFile)
    val cas = TypeScriptBuilder.compileWithModel(as)
    val grt = new GherkinRunner(new JavaScriptContext(cas))
    val run = grt.execute()
    println(new TestReport(run))
    run.result match {
      case _: Failed =>
      case wtf => fail(s"Unexpected: $wtf")
    }
  }
}

package com.atomist.rug.test.gherkin.handler

import com.atomist.project.archive.Rugs
import com.atomist.rug.runtime.js.RugContext
import com.atomist.rug.runtime.js.interop.jsPathExpressionEngine
import com.atomist.rug.test.gherkin.{AbstractExecutableFeature, Definitions, FeatureDefinition}
import com.atomist.source.ArtifactSource

class HandlerFeature(
                      definition: FeatureDefinition,
                      definitions: Definitions,
                      rugArchive: ArtifactSource,
                      rugs: Option[Rugs] = None)
  extends AbstractExecutableFeature[RugContext,HandlerScenarioWorld](definition, definitions) {

  private val rugContext: RugContext = new FakeRugContext("TEST_TEAM")

  override protected def createFixture: RugContext = rugContext

  override protected def createWorldForScenario(fixture: RugContext): HandlerScenarioWorld = {
    new HandlerScenarioWorld(definitions, fixture, rugs)
  }
}


class FakeRugContext(val teamId: String) extends RugContext {

  override val pathExpressionEngine: jsPathExpressionEngine = new jsPathExpressionEngine(this)
}


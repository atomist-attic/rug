package com.atomist.rug.test.gherkin.handler

import com.atomist.project.archive.Rugs
import com.atomist.rug.test.gherkin.{AbstractExecutableFeature, Definitions, FeatureDefinition}
import com.atomist.source.ArtifactSource
import com.atomist.tree.TreeNode

class HandlerFeature(
                      definition: FeatureDefinition,
                      definitions: Definitions,
                      rugArchive: ArtifactSource,
                      rugs: Option[Rugs] = None)
  extends AbstractExecutableFeature[TreeNode,HandlerScenarioWorld](definition, definitions) {

  override protected def createFixture =
    // TODO what do we do here?
    null
   // new ProjectMutableView(rugAs = rugArchive, originalBackingObject = EmptyArtifactSource())

  override protected def createWorldForScenario(fixture: TreeNode): HandlerScenarioWorld = {
    new HandlerScenarioWorld(definitions) //, fixture, rugs)
  }
}


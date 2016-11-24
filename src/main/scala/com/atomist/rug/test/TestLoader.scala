package com.atomist.rug.test

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.source.ArtifactSource

/**
  * Load test scenarios
  */
class TestLoader(val atomistConfig: AtomistConfig = DefaultAtomistConfig) {

  /**
    * Load test scenarios from this archive
    *
    * @param archive
    * @return
    */
  def loadTestScenarios(archive: ArtifactSource): Seq[TestScenario] = {
    archive.allFiles
      .filter(atomistConfig.isRugTest)
      .flatMap(f => ParserCombinatorTestScriptParser.parse(f))
  }

}

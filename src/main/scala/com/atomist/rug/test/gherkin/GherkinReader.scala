package com.atomist.rug.test.gherkin

import com.atomist.source.{ArtifactSource, FileArtifact}
import gherkin.ast.Feature
import gherkin.{AstBuilder, Parser}

/**
  * Parse ArtifactSources into Gherkin files
  */
object GherkinReader {

  val FeatureExtension = ".feature"

  private val parser = new Parser(new AstBuilder())

  /**
    * Find Gherkin features in this ArtifactSource. Must be compiled
    * if it contained TypeScript.
    */
  def findFeatures(as: ArtifactSource): Seq[FeatureDefinition] = {
    for {
      f <- as.allFiles
      if f.name.endsWith(FeatureExtension)
    }
      yield {
        //println(s"Looking at $f")
        val gherkinDocument = parser.parse(f.content)
        FeatureDefinition(gherkinDocument.getFeature, f, as)
      }
  }

}

/**
  * Return the feature and supporting information
  *
  * @param definition      definition file
  * @param feature         The Feature in Gherkin AST
  */
case class FeatureDefinition(
                              feature: Feature,
                              definition: FileArtifact,
                              as: ArtifactSource)

package com.atomist.project.archive

import com.atomist.project.ProjectOperation
import com.atomist.project.generate.{EditorInvokingProjectGenerator, ProjectGenerator}
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.AddressableRug
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator, RugDrivenProjectEditor}
import com.atomist.rug.spi.TypeRegistry
import com.atomist.rug.{DefaultRugPipeline, EmptyRugDslFunctionRegistry, Import}
import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging

import scala.collection.Seq



object ProjectOperationArchiveReaderUtils {

  def removeAtomistTemplateContent(startingProject: ArtifactSource, atomistConfig: AtomistConfig = DefaultAtomistConfig): ArtifactSource = {
    startingProject.filter(d => !d.path.equals(atomistConfig.atomistRoot), f => true)
  }
}

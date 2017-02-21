package com.atomist.project.archive

import com.atomist.project.ProjectOperation
import com.atomist.project.generate.{EditorInvokingProjectGenerator, ProjectGenerator}
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.Rug
import com.atomist.rug.runtime.rugdsl.{ContextAwareProjectOperation, DefaultEvaluator, Evaluator, RugDrivenProjectEditor}
import com.atomist.rug.spi.TypeRegistry
import com.atomist.rug.{DefaultRugPipeline, EmptyRugDslFunctionRegistry, Import}
import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging

import scala.collection.Seq

/**
  * Reads an archive and extracts all DSL based Rugs
  */
class RugDslArchiveReader(
                           atomistConfig: AtomistConfig = DefaultAtomistConfig,
                           evaluator: Evaluator = new DefaultEvaluator(new EmptyRugDslFunctionRegistry),
                           typeRegistry: TypeRegistry = DefaultTypeRegistry
                         )
  extends LazyLogging
    with RugArchiveReader[ProjectOperation] {

  private val oldInterpreterPipeline = new DefaultRugPipeline(typeRegistry, evaluator, atomistConfig)

  def findImports(startingProject: ArtifactSource): Seq[Import] = {
    oldInterpreterPipeline.parseRugFiles(startingProject).foldLeft(Nil: Seq[Import]) { (acc, rugProgram) => acc ++ rugProgram.imports }
  }

  override def find(startingProject: ArtifactSource,
                    namespace: Option[String],
                    otherOperations: Seq[Rug]): Rugs = {

    val otherProjectOperations = otherOperations.collect {
      case o: ProjectOperation => o
    }
    val operations = oldInterpreterPipeline.create(startingProject, namespace, otherProjectOperations)

    operations foreach {
      case capo: ContextAwareProjectOperation =>
        capo.setContext(operations ++ otherProjectOperations)
    }

    val editors = operations collect {
      case red: RugDrivenProjectEditor if red.program.publishedName.isEmpty => red
    }

    val generators = operations collect {
      case g: ProjectGenerator => g
      case red: RugDrivenProjectEditor if red.program.publishedName.isDefined =>
        // TODO want to pull up published name so it's not Rug only
        import ProjectOperationArchiveReaderUtils.removeAtomistTemplateContent
        val project: ArtifactSource = removeAtomistTemplateContent(startingProject)
        // TODO remove blanks in the generator names; we need to have a proper solution for this
        val name = red.program.publishedName.get
        logger.debug(s"Creating new generator with name $name")
        new EditorInvokingProjectGenerator(name, red, project)
    }

    val reviewers = operations collect {
      case r: ProjectReviewer => r
    }

    Rugs(editors, generators, reviewers, Nil, Nil, Nil)
  }
}

object ProjectOperationArchiveReaderUtils {

  def removeAtomistTemplateContent(startingProject: ArtifactSource, atomistConfig: AtomistConfig = DefaultAtomistConfig): ArtifactSource = {
    startingProject.filter(d => !d.path.equals(atomistConfig.atomistRoot), f => true)
  }
}

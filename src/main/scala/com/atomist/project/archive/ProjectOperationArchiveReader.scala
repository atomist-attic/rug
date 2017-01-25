package com.atomist.project.archive

import com.atomist.project.generate.{EditorInvokingProjectGenerator, ProjectGenerator}
import com.atomist.project.review.ProjectReviewer
import com.atomist.project.ProjectOperation
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.js.{JavaScriptProjectEditor, JavaScriptProjectOperationFinder}
import com.atomist.rug.runtime.rugdsl.{ContextAwareProjectOperation, DefaultEvaluator, Evaluator, RugDrivenProjectEditor}
import com.atomist.rug.spi.TypeRegistry
import com.atomist.rug.{DefaultRugPipeline, EmptyRugFunctionRegistry, Import}
import com.atomist.source.{ArtifactSource, FileArtifact}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.Seq

/**
  * Reads an archive and extracts Atomist project operations.
  * These can either be Rug DSL archives or TypeScript or JavaScript files.
  * Public API!
  */
class ProjectOperationArchiveReader(
                                     atomistConfig: AtomistConfig = DefaultAtomistConfig,
                                     evaluator: Evaluator = new DefaultEvaluator(new EmptyRugFunctionRegistry),
                                     typeRegistry: TypeRegistry = DefaultTypeRegistry
                                   )
  extends LazyLogging {

  val oldInterpreterPipeline = new DefaultRugPipeline(typeRegistry, evaluator, atomistConfig)

  def findImports(startingProject: ArtifactSource): Seq[Import] = {
    oldInterpreterPipeline.parseRugFiles(startingProject).foldLeft(Nil: Seq[Import]) { (acc, rugProgram) => acc ++ rugProgram.imports }
  }

  def findOperations(startingProject: ArtifactSource,
                     namespace: Option[String],
                     otherOperations: Seq[ProjectOperation]): Operations = {
    val fromTs = JavaScriptProjectOperationFinder.fromJavaScriptArchive(startingProject.filter(_ => true, (x: FileArtifact) => !atomistConfig.isJsHandler(x)))
    val fromOldPipeline = oldInterpreterPipeline.create(startingProject, namespace, otherOperations ++ fromTs)

    val operations = fromOldPipeline ++ fromTs

    operations foreach {
      case capo: ContextAwareProjectOperation =>
        capo.setContext(operations ++ otherOperations)
    }

    val editors = operations collect {
      case red: RugDrivenProjectEditor if red.program.publishedName.isEmpty => red

      // TODO these can't be generators yet.
      // This is a hack to avoid breaking tests
      case ed: JavaScriptProjectEditor => ed
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

    Operations(generators, editors, reviewers)
  }
}

object ProjectOperationArchiveReaderUtils {

  def removeAtomistTemplateContent(startingProject: ArtifactSource, atomistConfig: AtomistConfig = DefaultAtomistConfig): ArtifactSource = {
    startingProject.filter(d => !d.path.equals(atomistConfig.atomistRoot), f => true)
  }
}

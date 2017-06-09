package com.atomist.rug.test.gherkin.project

import com.atomist.param.{SimpleParameterValue, SimpleParameterValues}
import com.atomist.project.archive.Rugs
import com.atomist.project.common.InvalidParametersException
import com.atomist.project.edit.{FailedModificationAttempt, ModificationAttempt, ProjectEditor, SuccessfulModification}
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.RugNotFoundException
import com.atomist.rug.kind.core.{ProjectContext, ProjectMutableView, RepoResolver}
import com.atomist.rug.runtime.js.{BaseRugContext, JavaScriptProjectOperation}
import com.atomist.rug.test.gherkin._
import com.atomist.source.file.NamedFileSystemArtifactSourceIdentifier
import com.atomist.source.git.{FileSystemGitArtifactSource, GitRepositoryCloner}
import com.atomist.source.{ArtifactSource, EmptyArtifactSource}

import scala.util.{Failure, Success, Try}

/**
  * Convenient methods for working with projects
  */
class ProjectScenarioWorld(
                            definitions: Definitions,
                            rugs: Option[Rugs],
                            config: GherkinRunnerConfig)
  extends ScenarioWorld(definitions, rugs, config) {

  private var editorResults: Seq[Either[Throwable, ModificationAttempt]] = Nil

  private var project = new ProjectMutableView(rugAs = definitions.jsc.rugAs, originalBackingObject = EmptyArtifactSource("project-scenario-world"))

  override def target: AnyRef = project

  /**
    * Return the editor with the given name or throw an exception
    */
  def editor(name: String): ProjectEditor = {
    rugs match {
      case Some(r) =>
        r.editors.find(e => e.name == name) match {
          case Some(e) => e
          case _ => throw new RugNotFoundException(
            s"Editor with name '$name' can not be found in current context. Known editors are [${r.editorNames.mkString(", ")}]")
        }
      case _ => throw new RugNotFoundException("No context provided")
    }
  }

  def setProject(p: ProjectMutableView): Unit = {
    project = p
  }

  /**
    * Return the generator with the given name.
    * Throw an exception if it can't be found.
    */
  def generator(name: String): ProjectGenerator = {
    rugs match {
      case Some(r) =>
        r.generators.find(g => g.name == name) match {
          case Some(g) => g
          case _ => throw new RugNotFoundException(
            s"Generator with name '$name' can not be found in current context. Known generators are [${r.generatorNames.mkString(", ")}]")
        }
      case _ => throw new RugNotFoundException("No context provided")
    }
  }

  /**
    * Edit a project with the given editor, passed in from JavaScript.
    * We expect the JavaScript op to have been populated.
    */
  def generateWith(generator: ProjectGenerator, projectName: String, params: Any): Unit = {
    val resultAs = generator.generate(projectName, parameters(params), new ProjectContext(ProjectGenerationContext))
    project.updateTo(resultAs)
  }

  // For calling from Nashorn which doesn't like default parameter values!
  def generateWith(generator: ProjectGenerator, projectName: String): Unit = {
    generateWith(generator, projectName, null)
  }

  /**
    * Generate a project with the given editor, passed in from JavaScript.
    * We expect the JavaScript op to have been populated.
    */
  def editWith(editor: ProjectEditor, params: Any): Unit = {
    import scala.util.control.Exception._

    val editorResult = allCatch.either(editor.modify(project.currentBackingObject, parameters(params)))
    editorResults = editorResults :+ editorResult
    editorResult match {
      case Right(sm: SuccessfulModification) =>
        project.updateTo(sm.result)
      case Right(_) =>
      // We've already logged it. Do nothing
      case Left(ipe: InvalidParametersException) =>
        throw ipe
      case Left(unknown) =>
        throw unknown
    }
  }

  // For calling from nashorn which doesn't like default parameter values!
  def editWith(editor: ProjectEditor): Unit = {
    editWith(editor, null)
  }

  def modificationsMade: Boolean = editorResults.exists {
    case Right(_: SuccessfulModification) => true
    case _ => false
  }

  def failed: Boolean = editorResults.exists {
    case Left(_) => true
    case Right(_: FailedModificationAttempt) => true
    case _ => false
  }

  def editorsRun: Int = editorResults.size

  // TODO could probably pull this up
  @throws[InvalidParametersException]
  private def validateParams(op: JavaScriptProjectOperation): Unit = {
    // Pull parameters from script object mirror
    val paramValues = op.jsVar.keys().map(k => {
      SimpleParameterValue(k, op.jsVar.stringProperty(k, ""))
    })
    op.validateParameters(SimpleParameterValues(paramValues.toSeq))
  }
}

/**
  * Context that allows us to clone public github repos, which is
  * what we want in testing seed style generators
  */
private object ProjectGenerationContext extends BaseRugContext {

  override def repoResolver: Option[RepoResolver] = Some(GitRepoResolver)
}

private object GitRepoResolver extends RepoResolver {

  private val Cloner = GitRepositoryCloner()

  override def resolveBranch(owner: String, repoName: String, branch: String): ArtifactSource =
    Try(Cloner.clone(repo = repoName, owner = owner, branch = Some(branch))) match {
      case Success(dir) =>
        FileSystemGitArtifactSource(NamedFileSystemArtifactSourceIdentifier(repoName, dir))
      case Failure(e) =>
        throw new IllegalArgumentException("Failed to clone repo", e)
    }

  override def resolveSha(owner: String, repoName: String, sha: String): ArtifactSource =
    Try(Cloner.clone(repo = repoName, owner = owner, sha = Some(sha))) match {
      case Success(dir) =>
        FileSystemGitArtifactSource(NamedFileSystemArtifactSourceIdentifier(repoName, dir))
      case Failure(e) =>
        throw new IllegalArgumentException("Failed to clone repo", e)
    }
}

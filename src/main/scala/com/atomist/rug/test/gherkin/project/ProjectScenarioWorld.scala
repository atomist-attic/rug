package com.atomist.rug.test.gherkin.project

import com.atomist.param.{SimpleParameterValue, SimpleParameterValues}
import com.atomist.project.archive.Rugs
import com.atomist.project.common.InvalidParametersException
import com.atomist.project.edit.{FailedModificationAttempt, ModificationAttempt, ProjectEditor, SuccessfulModification}
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.RugNotFoundException
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.js.JavaScriptProjectOperation
import com.atomist.rug.runtime.js.interop.NashornUtils
import com.atomist.rug.test.gherkin._
import com.atomist.source.EmptyArtifactSource

/**
  * Convenient methods for working with projects
  */
class ProjectScenarioWorld(
                            definitions: Definitions,
                            rugs: Option[Rugs] = None)
  extends ScenarioWorld(definitions, rugs) {

  private var editorResults: Seq[Either[Throwable, ModificationAttempt]] = Nil


  val project = new ProjectMutableView(rugAs = definitions.jsc.rugAs, originalBackingObject = EmptyArtifactSource())

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
  def generateWith(generator: ProjectGenerator, params: Any): Unit = {
    val resultAs = generator.generate("project_name", parameters(params))
    project.updateTo(resultAs)
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
        ???
      case Left(unknown) =>
        throw unknown
    }
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
    val paramValues = op.jsVar.getOwnKeys(true).map(k => {
      SimpleParameterValue(k, NashornUtils.stringProperty(op.jsVar, k, ""))
    })
    op.validateParameters(SimpleParameterValues(paramValues))
  }

}

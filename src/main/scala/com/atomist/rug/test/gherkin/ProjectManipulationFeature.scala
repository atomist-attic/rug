package com.atomist.rug.test.gherkin

import com.atomist.param.{SimpleParameterValue, SimpleParameterValues}
import com.atomist.project.common.InvalidParametersException
import com.atomist.project.edit.{FailedModificationAttempt, ModificationAttempt, NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.js.interop.NashornUtils
import com.atomist.rug.runtime.js.{JavaScriptProjectEditor, JavaScriptProjectGenerator, JavaScriptProjectOperation}
import com.atomist.source.{ArtifactSource, EmptyArtifactSource}
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Executable feature that manipulates projects
  */
private[gherkin] class ProjectManipulationFeature(
                                                   definition: FeatureDefinition,
                                                   definitions: Definitions,
                                                   rugArchive: ArtifactSource)
  extends AbstractExecutableFeature[ProjectMutableView](definition, definitions) {

  override protected def createFixture = new ProjectMutableView(rugAs = rugArchive, originalBackingObject = EmptyArtifactSource())

  override protected def createWorldForScenario(fixture: ProjectMutableView): ScenarioWorld = {
    new ProjectScenarioWorld(definitions, fixture)
  }
}


/**
  * Convenient methods for working with projects
  */
class ProjectScenarioWorld(definitions: Definitions, project: ProjectMutableView)
  extends ScenarioWorld(definitions) {

  private var editorResults: Seq[Either[Throwable, ModificationAttempt]] = Nil

  /**
    * Edit a project with the given editor, passed in from JavaScript.
    * We expect the JavaScript op to have been populated.
    */
  def generateWith(gen: ScriptObjectMirror): Unit = {
    val generator = new JavaScriptProjectGenerator(definitions.jsc, jsVar = gen,
      rugAs = project.rugAs, startProject = project.rugAs, externalContext = Nil)
    validateParams(generator)
    val resultAs = generator.generate("project_name", SimpleParameterValues.Empty)
    project.updateTo(resultAs)
  }

  /**
    * Generate a project with the given editor, passed in from JavaScript.
    * We expect the JavaScript op to have been populated.
    */
  def editWith(ed: ScriptObjectMirror): Unit = {
    import scala.util.control.Exception._

    val editor = new JavaScriptProjectEditor(definitions.jsc, jsVar = ed,
      rugAs = project.rugAs, externalContext = Nil)
    validateParams(editor)

    val editorResult = allCatch.either(editor.modify(project.currentBackingObject, SimpleParameterValues.Empty))
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
    //println(s"EditorRuns=${editorResults}")
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

  /**
    * Last invalid parameters issue
    */
  def invalidParameters: InvalidParametersException = editorResults.collect {
    case Left(ipe: InvalidParametersException) => ipe
  }.lastOption.orNull

  def editorsRun: Int = editorResults.size

  // TODO could probably pull this up
  @throws[InvalidParametersException]
  private def validateParams(op: JavaScriptProjectOperation): Unit = {
    // Pull parameters from script object mirror
    val paramValues = op.jsVar.getOwnKeys(true).map(k => {
      //println(s"Found key: $k")
      SimpleParameterValue(k, NashornUtils.stringProperty(op.jsVar, k, ""))
    })
    op.validateParameters(SimpleParameterValues(paramValues))
  }

}

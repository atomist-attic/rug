package com.atomist.rug.test.gherkin

import com.atomist.param.{ParameterValues, SimpleParameterValue, SimpleParameterValues}
import com.atomist.project.archive.Rugs
import com.atomist.project.common.InvalidParametersException
import com.atomist.project.edit._
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.RugNotFoundException
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.js.interop.NashornUtils
import com.atomist.rug.runtime.js.JavaScriptProjectOperation
import com.atomist.source.{ArtifactSource, EmptyArtifactSource}
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Executable feature that manipulates projects
  */
private[gherkin] class ProjectManipulationFeature(
                                                   definition: FeatureDefinition,
                                                   definitions: Definitions,
                                                   rugArchive: ArtifactSource,
                                                   rugs: Option[Rugs] = None)
  extends AbstractExecutableFeature[ProjectMutableView](definition, definitions) {

  override protected def createFixture = new ProjectMutableView(rugAs = rugArchive, originalBackingObject = EmptyArtifactSource())

  override protected def createWorldForScenario(fixture: ProjectMutableView): ScenarioWorld = {
    new ProjectScenarioWorld(definitions, fixture, rugs)
  }
}


/**
  * Convenient methods for working with projects
  */
class ProjectScenarioWorld(definitions: Definitions, project: ProjectMutableView, rugs: Option[Rugs] = None)
  extends ScenarioWorld(definitions) {

  private var editorResults: Seq[Either[Throwable, ModificationAttempt]] = Nil

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

  private def parameters(params: Any): ParameterValues = {
    val m: Map[String, Object] = params match {
      case som: ScriptObjectMirror =>
        // The user has created a new JavaScript object, as in { foo: "bar" },
        // to pass up as an argument to the invoked editor. Extract its properties
        NashornUtils.extractProperties(som)
    }
    SimpleParameterValues(m)
  }

}

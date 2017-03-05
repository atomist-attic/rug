package com.atomist.rug.test.gherkin

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{FailedModificationAttempt, NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.js.{JavaScriptProjectEditor, JavaScriptProjectGenerator}
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

  override protected def createWorld(fixture: ProjectMutableView): World = {
    new ProjectWorld(definitions, fixture)
  }
}


/**
  * Convenient methods for working with projects
  */
class ProjectWorld(definitions: Definitions, project: ProjectMutableView)
  extends World(definitions) {

  /**
    * Edit a project with the given editor, passed in from JavaScript.
    * We expect the JavaScript op to have been populated.
    */
  def generateWith(gen: ScriptObjectMirror): Unit = {
    validateParams(gen)
    val generator = new JavaScriptProjectGenerator(definitions.jsc, jsVar = gen,
      rugAs = project.rugAs, startProject = project.rugAs, externalContext = Nil)
    val resultAs = generator.generate("project_name", SimpleParameterValues.Empty)
    project.updateTo(resultAs)
  }

  /**
    * Generate a project with the given editor, passed in from JavaScript.
    * We expect the JavaScript op to have been populated.
    */
  def editWith(ed: ScriptObjectMirror): Unit = {
    validateParams(ed)
    val editor = new JavaScriptProjectEditor(definitions.jsc, jsVar = ed,
      rugAs = project.rugAs, externalContext = Nil)
    editor.modify(project.currentBackingObject, SimpleParameterValues.Empty) match {
      case sm: SuccessfulModification =>
        project.updateTo(sm.result)
      case _: NoModificationNeeded =>
      case fm: FailedModificationAttempt =>
        throw new RuntimeException(s"Editor ${editor.name} failed with $fm")
    }
  }

  // TODO is this necessary?
  // what do we do if they're invalid?
  private def validateParams(op: ScriptObjectMirror): Unit = {
    // Pull parameters from script object mirror
    op.getOwnKeys(true).foreach(k => {
      println(s"Found key: $k")
    })
  }

}
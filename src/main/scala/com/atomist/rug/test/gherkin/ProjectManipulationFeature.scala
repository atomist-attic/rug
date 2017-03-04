package com.atomist.rug.test.gherkin

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.js.JavaScriptProjectGenerator
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
    * Generate a project with the given editor
    */
  def generateWith(gen: ScriptObjectMirror): Unit = {
    // Pull parameters from script object mirror
    gen.getOwnKeys(true).foreach(k => {
      println(s"Found key: $k")
    })
    val generator = new JavaScriptProjectGenerator(definitions.jsc, jsVar = gen,
      rugAs = project.rugAs, startProject = project.rugAs, externalContext = Nil)
    val resultAs = generator.generate("project_name", SimpleParameterValues.Empty)
    project.updateTo(resultAs)

  }

  def editWith(gen: ScriptObjectMirror): Unit = {
    ???
  }

}
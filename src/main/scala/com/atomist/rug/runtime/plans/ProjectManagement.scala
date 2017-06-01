package com.atomist.rug.runtime.plans

import com.atomist.param.ParameterValues
import com.atomist.project.edit.{ModificationAttempt, ProjectEditor}
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.spi.Handlers.EditorTarget
import com.atomist.source.ArtifactSource

/**
  * Implement this to do i/o operations for editors, generators etc
  */
trait ProjectManagement {

  /**
    * Run generator & persist the output from a project generator.
    */
  def generate(generator: ProjectGenerator, arguments: ParameterValues, projectName: String): ArtifactSource

  /**
    * Run the editor and take persistence decisions based on the results.
    */
  def edit(editor: ProjectEditor, arguments: ParameterValues, projectName: String, target: Option[EditorTarget]): ModificationAttempt
}

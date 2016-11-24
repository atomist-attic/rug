package com.atomist.rug

import com.atomist.project.ProjectOperation
import com.atomist.rug.runtime.RugDrivenProjectOperation
import com.atomist.source.{ArtifactSource, EmptyArtifactSource}

/**
  * Extended by classes than can compile a rug program into a ProjectOperation
  */
trait RugCompiler {

  /**
    * Compile a RugProgram into a ProjectEditor
    *
    * @param artifactSource backing artifact source for the operation.
    * @throws BadRugException if the program is semantically invalid
    * @return Operation for the program
    */
  @throws[BadRugException]
  def compile(rugProgram: RugProgram,
              artifactSource: ArtifactSource = EmptyArtifactSource(""),
              namespace: Option[String] = None,
              knownOperations: Seq[ProjectOperation] = Nil): RugDrivenProjectOperation
}

case class MissingFunction(name: String)

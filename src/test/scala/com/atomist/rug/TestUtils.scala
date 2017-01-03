package com.atomist.rug

import java.io.File

import com.atomist.project.ProjectOperationArguments
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.project.edit.{ModificationAttempt, ProjectEditor, SuccessfulModification}
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.source.file.{FileSystemArtifactSource, FileSystemArtifactSourceIdentifier}
import com.atomist.source.ArtifactSource
import org.scalatest.Matchers

object TestUtils extends Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  def doModification(program: ArtifactSource,
                     as: ArtifactSource,
                     backingAs: ArtifactSource,
                     poa: ProjectOperationArguments,
                     pipeline: RugPipeline = new DefaultRugPipeline(DefaultTypeRegistry)): ArtifactSource = {

    attemptModification(program, as, backingAs, poa, pipeline) match {
      case sm: SuccessfulModification =>
        // show(sm.result)
        sm.result
    }
  }

  def attemptModification(program: ArtifactSource,
                          as: ArtifactSource,
                          backingAs: ArtifactSource,
                          poa: ProjectOperationArguments,
                          pipeline: RugPipeline = new DefaultRugPipeline(DefaultTypeRegistry)): ModificationAttempt = {

    val eds = pipeline.create(backingAs + program, None)
    eds.size should be >= 1
    val pe = eds.head.asInstanceOf[ProjectEditor]
    pe.modify(as, poa)
  }

  // This brings in a node_modules directory that was copied there by a maven goal called copy-ts, which takes it from src/main/typescript
  val user_model = new FileSystemArtifactSource(FileSystemArtifactSourceIdentifier(new File("target/.atomist"))).withPathAbove(".atomist")

  val compiler = new TypeScriptCompiler()

  def compileWithModel(tsAs: ArtifactSource) : ArtifactSource = {
    compiler.compile(tsAs + user_model)
  }
}

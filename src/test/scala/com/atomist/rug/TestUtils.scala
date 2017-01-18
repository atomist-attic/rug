package com.atomist.rug

import java.io.File

import com.atomist.project.{ProjectOperationArguments, SimpleProjectOperationArguments}
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.project.edit.{ModificationAttempt, ProjectEditor, SuccessfulModification}
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.ts.TypeScriptInterfaceGenerator
import com.atomist.source.file.{FileSystemArtifactSource, FileSystemArtifactSourceIdentifier}
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource}
import jdk.nashorn.api.scripting.ScriptObjectMirror
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


  val compiler = new TypeScriptCompiler()

  // This brings in a node_modules directory that was copied there by a maven goal called copy-ts, which takes it from src/main/typescript
  val user_model: ArtifactSource = {

    val generator = new TypeScriptInterfaceGenerator
    val output = generator.generate(SimpleProjectOperationArguments("", Map(generator.OutputPathParam -> "Core.ts")))
    val src = new FileSystemArtifactSource(FileSystemArtifactSourceIdentifier(new File("src/main/typescript")))

    val compiled = compiler.compile(src.underPath("node_modules/@atomist").withPathAbove(".atomist") + output.withPathAbove(".atomist/rug/model"))
    compiled.underPath(".atomist").withPathAbove(".atomist/node_modules/@atomist")
  }

  def compileWithModel(tsAs: ArtifactSource) : ArtifactSource = {
    compiler.compile(user_model + tsAs)
  }
}

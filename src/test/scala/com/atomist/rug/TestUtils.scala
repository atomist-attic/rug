package com.atomist.rug

import com.atomist.project.ProjectOperationArguments
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.project.edit.{ModificationAttempt, ProjectEditor, SuccessfulModification}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.js.{JavaScriptProjectEditor, JavaScriptProjectOperationFinder}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.file.ClassPathArtifactSource
import com.atomist.source.{ArtifactSource, FileArtifact, FileEditor}
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
      case wtf => fail(s"Expected SuccessfulModication, not $wtf")
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

  /**
    * Compile the named TypeScript file in the package of the caller
    */
  def editorInSideFile(caller: Object, name: String): JavaScriptProjectEditor = {
    val resourcePath = caller.getClass.getPackage.getName.replace(".", "/")
    //println(s"Using resourcePath [$resourcePath]")
    val raw = ClassPathArtifactSource.toArtifactSource(
      resourcePath
    )
    if (raw.empty) {
      fail(s"Can't load resources at class path resource [$resourcePath]")
    }
    val tsAs = raw.filter(d => true, f => f.name == name)
    if (tsAs.empty) {
      fail(s"Can't load resource named [$name] at class path resource [$resourcePath]")
    }
    val withAtomistDir = tsAs.edit(new FileEditor {
      override def canAffect(f: FileArtifact) = true
      // Put the editor in the .atomist directory so it's found
      override def edit(f: FileArtifact) = f.withPath(".atomist/editors/" + f.path)
    })
    val as = TypeScriptBuilder.compileWithModel(withAtomistDir)
    val eds = JavaScriptProjectOperationFinder.fromJavaScriptArchive(as)
    eds.head.asInstanceOf[JavaScriptProjectEditor]
  }
}

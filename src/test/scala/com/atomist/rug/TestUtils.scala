package com.atomist.rug

import com.atomist.param.{ParameterValues, SimpleParameterValues}
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig, Rugs, SimpleJavaScriptProjectOperationFinder}
import com.atomist.project.edit.{ModificationAttempt, ProjectEditor, SuccessfulModification}
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source._
import com.atomist.source.file.ClassPathArtifactSource
import org.scalatest.Matchers

object TestUtils extends Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  def doModification(program: ArtifactSource,
                     as: ArtifactSource,
                     backingAs: ArtifactSource,
                     poa: ParameterValues,
                     pipeline: RugPipeline = new DefaultRugPipeline(DefaultTypeRegistry)): ArtifactSource = {

    attemptModification(program, as, backingAs, poa, pipeline) match {
      case sm: SuccessfulModification => sm.result
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  private def isTypeScript(program: ArtifactSource) =
    program.allFiles.exists(
      f => f.name.endsWith(".ts") && f.path.startsWith(".atomist/editors")
    )

  def attemptModification(program: ArtifactSource,
                          as: ArtifactSource,
                          backingAs: ArtifactSource,
                          poa: ParameterValues,
                          pipeline: RugPipeline = new DefaultRugPipeline(DefaultTypeRegistry)): ModificationAttempt = {
    val pe: ProjectEditor =
      if (isTypeScript(program)) {
        val as = TypeScriptBuilder.compileWithModel(program)
        SimpleJavaScriptProjectOperationFinder.find(as).editors.head
      }
      else {
        // Rug editor
        val eds = pipeline.create(backingAs + program, None)
        eds.size should be >= 1
        eds.head.asInstanceOf[ProjectEditor]
      }
    pe.modify(as, poa)
  }

  /**
    * Update with the given program
    * @param prog Rug or TypeScript program
    * @param project project to update
    * @param params parameters. Default is none
    * @return result of the modification attempt
    */
  def updateWith(prog: String, project: ArtifactSource, params: Map[String,Object] = Map()): ModificationAttempt = {
    val pas = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog))
    attemptModification(pas, project, EmptyArtifactSource(""), SimpleParameterValues(params))
  }

  /**
    * Compile the named TypeScript file in the package of the caller
    */
  def editorInSideFile(caller: Object, name: String): ProjectEditor = {
    rugsInSideFile(caller, name).editors.head
  }

  def reviewerInSideFile(caller: Object, name: String): ProjectReviewer = {
    rugsInSideFile(caller, name).reviewers.head
  }

  /**
    * Return all resources in this package as an ArtifactSource
    */
  def resourcesInPackage(caller: Object): ArtifactSource = {
    val resourcePath = caller.getClass.getPackage.getName.replace(".", "/")
    // println(s"Using resourcePath [$resourcePath]")
    val raw = ClassPathArtifactSource.toArtifactSource(resourcePath)
    if (raw.empty) {
      fail(s"Can't load resources at class path resource [$resourcePath]")
    }
    raw
  }

  def rugsInSideFile(caller: Object, names: String*): Rugs = {
    val raw = resourcesInPackage(this)
    val tsAs = raw.filter(_ => true, f => names.contains(f.name))
    if (tsAs.empty) {
      fail(s"Can't load resources named [$names] at class path resource in package [${caller.getClass.getPackage.getName}]")
    }
    val withAtomistDir = tsAs.edit(new FileEditor {
      override def canAffect(f: FileArtifact) = true

      // Put the editor in the .atomist directory so it's found
      override def edit(f: FileArtifact) = f.withPath(".atomist/editors/" + f.path)
    })
    val as = TypeScriptBuilder.compileWithModel(withAtomistDir)
    SimpleJavaScriptProjectOperationFinder.find(as)
  }
}

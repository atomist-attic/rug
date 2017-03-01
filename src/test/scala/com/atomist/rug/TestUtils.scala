package com.atomist.rug

import com.atomist.param.{ParameterValues, SimpleParameterValues}
import com.atomist.project.ProjectOperation
import com.atomist.project.archive._
import com.atomist.project.common.MissingParametersException
import com.atomist.project.edit.{Applicability, ModificationAttempt, ProjectEditor, SuccessfulModification}
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.rug.runtime.{AddressableRug, Rug}
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
    program.allFiles.exists(f => f.name.endsWith(".ts") && f.path.startsWith(".atomist/editors"))

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
        fail("Rug DSL is no longer supported")
      }
    pe.modify(as, poa)
  }

  /**
    * Update with the given program.
    *
    * @param prog    Rug or TypeScript program
    * @param project project to update
    * @param params  parameters. Default is none
    * @return result of the modification attempt
    */
  def updateWith(prog: String, project: ArtifactSource, params: Map[String, Object] = Map()): ModificationAttempt = {
    val pas = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog))
    attemptModification(pas, project, EmptyArtifactSource(""), SimpleParameterValues(params))
  }

  /**
    * Compile the named TypeScript file in the package of the caller
    */
  def editorInSideFile(caller: Object, name: String): ProjectEditor =
    rugsInSideFile(caller, name).editors.head

  def reviewerInSideFile(caller: Object, name: String): ProjectReviewer =
    rugsInSideFile(caller, name).reviewers.head

  /**
    * Return all resources in this package as an ArtifactSource
    */
  def resourcesInPackage(caller: Object): ArtifactSource = {
    val resourcePath = caller.getClass.getPackage.getName.replace(".", "/")
    //println(s"Using resourcePath [$resourcePath] for $caller")
    val raw = ClassPathArtifactSource.toArtifactSource(resourcePath)
    if (raw.empty) {
      fail(s"Can't load resources at class path resource [$resourcePath]")
    }
    raw
  }

  def rugsInSideFile(caller: Object, names: String*): Rugs = {
    val as = rugsInSideFileAsArtifactSource(caller, names:_*)
    SimpleJavaScriptProjectOperationFinder.find(as)
  }

  def rugsInSideFileAsArtifactSource(caller: Object, names: String*): ArtifactSource = {
    val raw = resourcesInPackage(caller)
    val tsAs = raw.filter(_ => true, f => names.contains(f.name))
    if (tsAs.empty) {
      fail(s"Can't load ANYTHING from resources named [${names.mkString(",")}] at class path resource in package [${caller.getClass.getPackage.getName}]")
    }
    val withAtomistDir = tsAs.edit(new FileEditor {
      override def canAffect(f: FileArtifact) = true

      // Put the editor in the .atomist directory so it's found
      override def edit(f: FileArtifact) = f.withPath(".atomist/editors/" + f.path)
    })
    TypeScriptBuilder.compileWithModel(withAtomistDir)
  }

  /**
    * Make a rug addressable
    *
    * @param rug
    * @return
    */
  def addressableEditor(rug: ProjectEditor, _artifact: String = "artifact", _group: String = "foo", _version: String = "1.2.3"): AddressableRug = {
    new AddressableRug with ProjectEditor {

      override def artifact = _artifact

      override def group = _group

      override def version = _version

      override def description = rug.description

      override def name = rug.name

      override def tags = rug.tags

      override def modify(as: ArtifactSource, poa: ParameterValues): ModificationAttempt = rug.modify(as, poa)

      override def applicability(as: ArtifactSource): Applicability = rug.applicability(as)
    }
  }
}

/**
  * Convenience, but expensive as JavaScriptContext is not reused
  * Also namespace/otherops not used. Really only for test/fun.
  */
object SimpleJavaScriptProjectOperationFinder {
  def find(as: ArtifactSource): Rugs = {
    new JavaScriptProjectOperationFinder(new JavaScriptContext(as)).find(Nil)
  }
}

package com.atomist.rug

import com.atomist.param.{ParameterValues, SimpleParameterValues}
import com.atomist.project.archive._
import com.atomist.project.edit.{ModificationAttempt, ProjectEditor, SuccessfulModification}
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source._
import com.atomist.source.file.ClassPathArtifactSource
import org.scalatest.Matchers

import scala.util.matching.Regex

object RugArchiveReader {
  def apply(as: ArtifactSource): Rugs = SimpleRugResolver(as).resolvedDependencies.rugs
}

object SimpleRugResolver {
  def apply(as: ArtifactSource) : RugResolver = new ArchiveRugResolver(Dependency(as))
}

object TestUtils extends Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  /**
    * Find a bad pattern in the given ArtifactSource and fail with a
    * list and then a dump of the files containing it
    */
  def failOnFindingPattern(as: ArtifactSource,
                           badThingName: String,
                           regex: Regex,
                           fileFilter: FileArtifact => Boolean = _ => true): Unit = {
    val badFiles = as.allFiles.filter(fileFilter) collect {
      case f if regex.findFirstIn(f.content).isDefined =>
        //println(regex.findAllIn(f.content).mkString("\n"))
        f
    }
    if (badFiles.nonEmpty) {
      fail(s"$badThingName:\n\n" + badFiles.map(_.path).mkString("\n")
        + "\n" + badFiles.map(f => f.path + "\n" + f.content).mkString("\n"))
    }
  }

  def doModification(program: ArtifactSource,
                     as: ArtifactSource,
                     backingAs: ArtifactSource,
                     poa: ParameterValues): ArtifactSource = {

    attemptModification(program, as, backingAs, poa) match {
      case sm: SuccessfulModification => sm.result
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  private def isTypeScript(program: ArtifactSource) =
    program.allFiles.exists(f => f.name.endsWith(".ts") && f.path.startsWith(".atomist/editors"))

  def attemptModification(program: ArtifactSource,
                          as: ArtifactSource,
                          backingAs: ArtifactSource,
                          poa: ParameterValues): ModificationAttempt = {
    val pe: ProjectEditor =
      if (isTypeScript(program)) {
        val as = TypeScriptBuilder.compileWithModel(program)
        RugArchiveReader(as).editors.head
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
    val pas = new SimpleFileBasedArtifactSource("", StringFileArtifact(".atomist/editors/Default.ts", prog))
    attemptModification(pas, project, EmptyArtifactSource(""), SimpleParameterValues(params))
  }

  /**
    * Compile the named TypeScript file in the package of the caller.
    */
  def editorInSideFile(caller: AnyRef, name: String): ProjectEditor =
    rugsInSideFile(caller, name).editors.head

  def reviewerInSideFile(caller: AnyRef, name: String): ProjectReviewer =
    rugsInSideFile(caller, name).reviewers.head

  /**
    * Return all resources in this package as an ArtifactSource.
    */
  def resourcesInPackage(caller: AnyRef): ArtifactSource = {
    val resourcePath = caller.getClass.getPackage.getName.replace(".", "/")
    val raw = ClassPathArtifactSource.toArtifactSource(resourcePath)
    if (raw.empty) {
      fail(s"Can't load resources at class path resource [$resourcePath]")
    }
    raw
  }

  /**
    * Return a specific file from the classpath (usually, src/test/resources)
    * in the package of the caller
    */
  def fileInPackage(caller: AnyRef, name: String, pathAbove: String = ""): Option[FileArtifact] = {
    resourcesInPackage(caller).allFiles.find(_.name == name).map(_.withPath(pathAbove + "/" + name))
  }

  def requiredFileInPackage(caller: AnyRef, name: String, pathAbove: String = ""): FileArtifact =
    fileInPackage(caller, name, pathAbove).getOrElse(throw new IllegalArgumentException(s"Cannot find file [$name] in [${caller.getClass.getPackage.getName}]"))

  def contentOf(caller: AnyRef, name: String): String =
    requiredFileInPackage(this, name).content

  def rugsInSideFile(caller: AnyRef, names: String*): Rugs = {
    val as = rugsInSideFileAsArtifactSource(caller, names: _*)
    RugArchiveReader(as)
  }

  def rugsInSideFileAsArtifactSource(caller: AnyRef, names: String*): ArtifactSource = {
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
}

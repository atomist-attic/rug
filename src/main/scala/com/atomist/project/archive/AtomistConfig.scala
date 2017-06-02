package com.atomist.project.archive

import com.atomist.source.{ArtifactSource, FileArtifact}

/**
  * Holds all configuration related to Rug archive project structure, such
  * as required paths for different kinds of artifacts.
  */
trait AtomistConfig {

  def atomistRoot: String

  /**
    * Editors directory under Atomist root or root of archive.
    */
  def editorsDirectory: String

  /**
    * Generators directory under Atomist root or root of archive.
    */
  def generatorsDirectory: String

  /**
    * Templates directory under Atomist root or root of archive.
    */
  def templatesDirectory: String

  /**
    * Tests directory under Atomist root or root of archive.
    */
  def testsDirectory: String

  /**
    * Handlers directory under Atomist root or root of archive.
    */
  def handlersDirectory: String

  def jsExtension: String

  def editorsRoot = s"$atomistRoot/$editorsDirectory"

  def generatorsRoot = s"$atomistRoot/$generatorsDirectory"

  def templatesRoot = s"$atomistRoot/$templatesDirectory"

  def handlersRoot = s"$atomistRoot/$handlersDirectory"

  def testsRoot = s"$atomistRoot/$testsDirectory"

  /**
    * Return the atomist content only
    *
    * @param rugArchive artifact source
    * @return only the Atomist content from the archive
    */
  def atomistContent(rugArchive: ArtifactSource): ArtifactSource = {
    // Find files in dir (if any)
    def files(dir: String): Seq[FileArtifact] = {
      rugArchive.findDirectory(dir) match {
        case Some(found) => found.allFiles
        case _ => Nil
      }
    }

    val atomistFiles = files(editorsRoot) ++ files(generatorsRoot) ++ files(handlersRoot) ++ files(testsRoot)
    ArtifactSource.fromFiles(atomistFiles: _*)
  }

  def isJsSource(f: FileArtifact): Boolean = {
    f.name.endsWith(jsExtension) && isAtomistSource(f)
  }

  def isJsTest(f: FileArtifact): Boolean = {
    // TODO fix hard coding
    f.name.endsWith(jsExtension) && f.path.startsWith(s"$atomistRoot/test")
  }

  def isAtomistSource(f: FileArtifact): Boolean = {
    f.path.startsWith(editorsRoot) ||
      f.path.startsWith(generatorsRoot) ||
      f.path.startsWith(handlersRoot) ||
      f.path.startsWith(handlersDirectory) ||
      f.path.startsWith(editorsDirectory) ||
      f.path.startsWith(generatorsDirectory)
  }

  def isJsHandler(f: FileArtifact): Boolean = {
    f.name.endsWith(jsExtension) && (f.path.startsWith(handlersRoot) ||
      f.path.startsWith(handlersDirectory))
  }

  def templateContentIn(rugAs: ArtifactSource): ArtifactSource =
    rugAs / templatesRoot + rugAs / templatesDirectory

}

object DefaultAtomistConfig extends AtomistConfig {

  override val atomistRoot = ".atomist"

  override val editorsDirectory = "editors"

  override val generatorsDirectory = "generators"

  override val templatesDirectory = "templates"

  override val handlersDirectory = "handlers"

  override val testsDirectory = "tests"

  override val jsExtension = ".js"
}

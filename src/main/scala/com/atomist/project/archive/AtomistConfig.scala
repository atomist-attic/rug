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
    * Reviewers directory under Atomist root or root of archive.
    */
  def reviewersDirectory: String

  /**
    * Executors directory under Atomist root or root of archive.
    */
  def executorsDirectory: String

  /**
    * Handlers directory under Atomist root or root of archive.
    */
  def handlersDirectory: String

  /**
    * Extension for Rug files.
    */
  def rugExtension: String

  def testExtension: String

  def jsExtension: String

  def editorsRoot = s"$atomistRoot/$editorsDirectory"

  def generatorsRoot = s"$atomistRoot/$generatorsDirectory"

  def templatesRoot = s"$atomistRoot/$templatesDirectory"

  def reviewersRoot = s"$atomistRoot/$reviewersDirectory"

  def executorsRoot = s"$atomistRoot/$executorsDirectory"

  def handlersRoot = s"$atomistRoot/$handlersDirectory"

  def testsRoot = s"$atomistRoot/$testsDirectory"

  val defaultRugFileBase = "default"

  /**
    * Default path of Rug program to create when a string is passed in.
    */
  def defaultRugFilepath = s"$editorsRoot/$defaultRugFileBase$rugExtension"

  /**
    * Default path of TypeScript program to create when a string is passed in.
    */
  def defaultTypeScriptFilepath = s"$editorsRoot/$defaultRugFileBase.ts"

  /**
    * Return the atomist content only
    * @param rugArchive artifact source
    * @return only the Atomist content from the archive
    */
  def atomistContent(rugArchive: ArtifactSource): ArtifactSource =
    rugArchive.filter(d => d.path.startsWith(atomistRoot), f => f.path.startsWith(atomistRoot))

  def isRugSource(f: FileArtifact): Boolean = {
    f.name.endsWith(rugExtension) && isAtomistSource(f)

  }

  def isJsSource(f:FileArtifact): Boolean = {
    f.name.endsWith(jsExtension) && isAtomistSource(f)
  }

  def isJsTest(f:FileArtifact): Boolean = {
    // TODO fix hard coding
    f.name.endsWith(jsExtension) && f.path.startsWith(s"$atomistRoot/test")
  }

  def isAtomistSource(f: FileArtifact): Boolean = {
      f.path.startsWith(editorsRoot) ||
      f.path.startsWith(generatorsRoot) ||
      f.path.startsWith(reviewersRoot) ||
      f.path.startsWith(executorsRoot) ||
      f.path.startsWith(handlersRoot) ||
      f.path.startsWith(handlersDirectory) ||
      f.path.startsWith(editorsDirectory) ||
      f.path.startsWith(generatorsDirectory) ||
      f.path.startsWith(reviewersDirectory) ||
      f.path.startsWith(executorsDirectory)
  }

  def isRugTest(f: FileArtifact): Boolean = {
    f.name.endsWith(testExtension) && (
      f.path.startsWith(testsRoot) ||
        f.path.startsWith(testsDirectory)
      )
  }

  def isJsHandler(f: FileArtifact): Boolean = {
    f.name.endsWith(jsExtension) &&  (f.path.startsWith(handlersRoot) ||
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

  override val reviewersDirectory = "reviewers"

  override val executorsDirectory = "executors"

  override val handlersDirectory = "handlers"

  override val testsDirectory = "tests"

  override val rugExtension = ".rug"

  override val jsExtension = ".js"

  override def testExtension: String = ".rt"
}

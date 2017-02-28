package com.atomist.rug

import com.atomist.project.ProjectOperation
import com.atomist.project.archive.AtomistConfig
import com.atomist.rug.runtime.AddressableRug
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}

/**
  * Interface for compiling Rug files
  */
trait RugPipeline {

  def atomistConfig: AtomistConfig

  /**
    * Parse programs in the archive
    *
    * @param rugArchive      the artifact source
    * @param knownOperations known other operations we might reference
    * @throws BadRugException
    * @throws IllegalArgumentException
    * @return a sequence of ProjectOperations
    */
  @throws[BadRugException]
  @throws[IllegalArgumentException]
  def create(rugArchive: ArtifactSource,
             knownOperations: Seq[AddressableRug] = Nil): Seq[ProjectOperation]

  @throws[BadRugException]
  @throws[IllegalArgumentException]
  final def createFromString(input: String,
                             otherOperations: Seq[AddressableRug] = Nil): Seq[ProjectOperation] = {
    import InterpreterRugPipeline._

    val as =
      new SimpleFileBasedArtifactSource(DefaultRugArchive,
        StringFileArtifact(defaultFilenameFor(input), input))
    create(as, otherOperations)
  }
  /**
    * Determine if this program is Rug or TypeScript and name the file
    * we create accordingly
    * @param prog
    * @return
    */
  def defaultFilenameFor(prog: String): String = prog match {
    case _ if prog.contains("import {") =>
      atomistConfig.defaultTypeScriptFilepath
    case _ =>
      atomistConfig.defaultRugFilepath
  }

  @throws[BadRugPackagingException]
  def validatePackaging(rugFiles: Seq[FileArtifact], f: FileArtifact, progs: Seq[RugProgram]): Unit
}

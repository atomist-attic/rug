package com.atomist.rug

import com.atomist.project.ProjectOperation
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.parser.{ParserCombinatorRugParser, RugParser}
import com.atomist.rug.runtime.{AddressableRug, Rug}
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.TypeRegistry
import com.atomist.source.{ArtifactSource, FileArtifact}

/**
  * Build executable ProjectOperations from Rug archives
  * Rug scripts must be in .atomist/editors directory
  *
  * If you're looking for "how do I parse these rugs" you probably want ProjectOperationArchiveReader,
  * which calls into this and does other important stuff too
  *
  * @param parser parser to use
  * @param compiler compiler to use
  */
class InterpreterRugPipeline(
                              parser: RugParser,
                              compiler: RugCompiler,
                              val atomistConfig: AtomistConfig)
  extends RugPipeline {

  @throws[BadRugException]
  @throws[IllegalArgumentException]
  override def create(rugArchive: ArtifactSource,
                      knownOperations: Seq[AddressableRug]): Seq[ProjectOperation] = {
    val rugCompilationUnits = parseRugFiles(rugArchive)
    compileRugPrograms(rugCompilationUnits, rugArchive, knownOperations)
  }

  def parseRugFiles(rugArchive: ArtifactSource): Seq[RugProgram] = {
    val rugFiles = rugArchive.allFiles
      .filter(atomistConfig.isRugSource)
    rugFiles.flatMap(f => {
      val progs = parser.parse(f)
      validatePackaging(rugFiles, f, progs)
      progs
    })
  }

  def compileRugPrograms(rugCompilationUnits: Seq[RugProgram],
                         rugArchive: ArtifactSource,
                         knownOperations: Seq[AddressableRug] = Nil): Seq[ProjectOperation] = {
    val progs = rugCompilationUnits.map(program => compiler.compile(program, rugArchive, knownOperations))
    //tell each program in the archive about the others
    progs.foreach(p => p.addToArchiveContext(progs.filter(other => p != other)))
    progs
  }

  @throws[BadRugPackagingException]
  override def validatePackaging(rugFiles: Seq[FileArtifact], f: FileArtifact, progs: Seq[RugProgram]): Unit = {
    (f, progs) match {
      case (`f`, _) if f.path.equals(atomistConfig.defaultRugFilepath) =>
      // It's OK to pass anything in via the convenient method taking string
      case (_, (prog: RugProgram) :: Nil) =>
        val expectedFilename = progs.head.name + atomistConfig.rugExtension
        if (!f.name.equals(expectedFilename))
          throw new BadRugPackagingException(s"Program ${progs.head.name} must be in file named $expectedFilename, not ${f.name}", f, progs)
      case (`f`, (prog: RugProgram) :: moreProgs) =>
        val nonPublicProgramsInThisFile = progs.filter(p => !f.name.contains(p.name))
        if (nonPublicProgramsInThisFile.size == progs.size)
          throw new BadRugPackagingException(s"Operation [${nonPublicProgramsInThisFile.map(_.name).mkString(",")}]" +
            s" in file '${f.path}' does not have the same name as the file and is referenced outside file.", f, progs)
        for {
          otherFile <- rugFiles
          if otherFile != f
          nonPublicProgram <- nonPublicProgramsInThisFile
          nonPublicProgramName = nonPublicProgram.name
          if otherFile.content.contains(nonPublicProgramName)
        }
          throw new BadRugPackagingException(s"Operation $nonPublicProgramName in file $f do have the same name as the file and is referenced outside file.", f, progs)
    }
    if (f.path.contains(atomistConfig.reviewersRoot) && progs.exists(ed => !ed.isInstanceOf[RugReviewer])) {
      throw new BadRugPackagingException(s"Found editor(s) in file $f in ${atomistConfig.reviewersRoot} reviewers tree. It must be under ${atomistConfig.editorsRoot}.",
        f, progs)
    }
  }
}

object InterpreterRugPipeline {

  val DefaultRugArchive = "Rug sources"
}

class DefaultRugPipeline(
                          kindRegistry: TypeRegistry = DefaultTypeRegistry,
                          evaluator: Evaluator = new DefaultEvaluator(new EmptyRugDslFunctionRegistry),
                          atomistConfig: AtomistConfig = DefaultAtomistConfig)
  extends InterpreterRugPipeline(
    new ParserCombinatorRugParser,
    new DefaultRugCompiler(evaluator, kindRegistry),
    atomistConfig
  )

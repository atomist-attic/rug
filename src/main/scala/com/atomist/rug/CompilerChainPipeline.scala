package com.atomist.rug

import com.atomist.project.ProjectOperation
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.compiler.Compiler
import com.atomist.rug.runtime.js.JavaScriptProjectOperationFinder
import com.atomist.rug.runtime.rugdsl.ContextAwareProjectOperation
import com.atomist.source.{ArtifactSource, FileArtifact}

/**
  * Chain compilers, running all and examining resulting JavaScript
  */
class CompilerChainPipeline(compilers: Seq[Compiler],
                            val atomistConfig: AtomistConfig = DefaultAtomistConfig)
  extends RugPipeline {

  private val comps: Seq[ArtifactSource => ArtifactSource] =
    compilers.map(c => c.compile(_))

  @throws[BadRugException]
  @throws[IllegalArgumentException]
  override def create(rugArchive: ArtifactSource,
                      namespace: Option[String],
                      knownOperations: Seq[ProjectOperation] = Nil): Seq[ProjectOperation] = {
    val ops = JavaScriptProjectOperationFinder.fromJavaScriptArchive(comps.reduce(_ compose _)(rugArchive))
    ops foreach {
      case capo: ContextAwareProjectOperation =>
        capo.setContext(ops)
    }
    ops
  }

  @throws[BadRugPackagingException]
  override def validatePackaging(rugFiles: Seq[FileArtifact], f: FileArtifact, progs: Seq[RugProgram]): Unit = {

  }

}

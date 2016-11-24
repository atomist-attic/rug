package com.atomist.rug.runtime.manager

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig, ProjectOperationArchiveReader}
import com.atomist.project.{Executor, ProjectOperationArguments}

object DefaultRuntimeManager extends RuntimeManagerSupport {

  override val atomistConfig: AtomistConfig = DefaultAtomistConfig

  override val archiveReader: ProjectOperationArchiveReader = new ProjectOperationArchiveReader(atomistConfig)

  override def executeWith(ex: Executor, uc: UserContext, poa: ProjectOperationArguments): Unit = ???
}
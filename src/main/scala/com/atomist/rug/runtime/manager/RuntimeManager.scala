package com.atomist.rug.runtime.manager

import com.atomist.project.{Executor, ProjectOperation, ProjectOperationArguments}
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig, Operations}
import com.atomist.rug.{BadRugException, RugRuntimeException}
import com.atomist.source.ArtifactSource

import scala.collection.Seq

trait RuntimeManager {

  def atomistConfig: AtomistConfig = DefaultAtomistConfig

  @throws[BadRugException]
  @throws[RugRuntimeException]
  def loadArchive(artifactSource: ArtifactSource, namespace: Option[String]): Seq[ProjectOperation]

  // TODO is this the right signature?
  def removeArchive(artifactSource: ArtifactSource)

  /**
    * Forget all operations.
    */
  def clear()

  /**
    * Return the operations known to this manager.
    */
  def operations: Operations

  @throws[BadRugException]
  @throws[RugRuntimeException]
  def execute(name: String, uc: UserContext, poa: ProjectOperationArguments) {
    executeWith(operations.findExecutor(name), uc, poa)
  }

  @throws[BadRugException]
  @throws[RugRuntimeException]
  def executeWith(ex: Executor, uc: UserContext, poa: ProjectOperationArguments)
}

package com.atomist.rug.runtime.manager

import com.atomist.project.ProjectOperation
import com.atomist.project.archive._
import com.atomist.rug.{BadRugException, RugRuntimeException}
import com.atomist.source.ArtifactSource

/**
  * Convenient management for RugRuntime.
  * Callers should use the convenient execute, generate, review and edit methods to ensure
  * appropriate behavior wrt setup etc.
  */
abstract class RuntimeManagerSupport extends RuntimeManager {

  def archiveReader: ProjectOperationArchiveReader

  protected def InitialOperations = CoreProjectOperations(Some("atomist.core"))

  private var _operations = InitialOperations

  @throws[BadRugException]
  @throws[RugRuntimeException]
  override def loadArchive(rugArchive: ArtifactSource, namespace: Option[String]): Seq[ProjectOperation] = {
    val found = archiveReader.findOperations(rugArchive, namespace, _operations.allOperations)
    _operations += found
    found.allOperations
  }

  // TODO is the right signature
  override def removeArchive(rugArchive: ArtifactSource): Unit = {
    ???
  }

  override def clear(): Unit = {
    _operations = InitialOperations
  }

  /**
    * Return the operations known to this manager.
    */
  override def operations: Operations = _operations
}

package com.atomist.project.edit

import com.atomist.project.ProjectOperationArguments
import com.atomist.project.common.support.ProjectOperationParameterSupport
import com.atomist.source.ArtifactSource

/**
  * Uses template method pattern to add applicability check
  * and to check whether the post condition is already satisfied.
  */
trait ProjectEditorSupport
  extends ProjectEditor
  with ProjectOperationParameterSupport {

  /**
    * Should we fail if we were called are are not applicable?
    *
    * @return whether to fail if not applicable
    */
  def failOnNotApplicable: Boolean = false

  /**
    * Should we fail if no modifications were made.
    *
    * @return whether to fail if no modifications were made
    */
  def failOnNoModification: Boolean = false

  override def modify(as: ArtifactSource, args: ProjectOperationArguments): ModificationAttempt = {
    val poa = addDefaultParameterValues(args)
    validateParameters(poa)

    val r =
      if (!applicability(as).canApply) failOnNotApplicable match {
        case true => FailedModificationAttempt(s"Can't apply $this to ${as.getIdString}")
        case false => NoModificationNeeded(s"Can't apply $this to ${as.getIdString}")
      } else if (meetsPostcondition(as)) {
        NoModificationNeeded(s"Artifact source meets postcondition already")
      } else {
        modifyInternal(as, poa)
      }

    // We may need to make it fail-fast
    r match {
      case nmn: NoModificationNeeded if failOnNoModification =>
        FailedModificationAttempt(nmn.comment + " and no modification not permitted")
      case x => x
    }
  }

  protected def modifyInternal(as: ArtifactSource, pmi: ProjectOperationArguments): ModificationAttempt
}

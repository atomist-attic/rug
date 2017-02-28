package com.atomist.project.edit

import com.atomist.param.ParameterValues
import com.atomist.source.ArtifactSource

/**
  * Uses template method pattern to add applicability check
  * and to check whether the post condition is already satisfied.
  */
trait ProjectEditorSupport
  extends ProjectEditor {

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

  def modify(as: ArtifactSource, args: ParameterValues): ModificationAttempt = {
    val poa = addDefaultParameterValues(args)
    validateParameters(poa)

    val r = if (!applicability(as).canApply) {
      if (failOnNotApplicable) {
        FailedModificationAttempt(s"Can't apply $this to ${as.getIdString}")
      } else {
        NoModificationNeeded(s"Can't apply $this to ${as.getIdString}")
      }
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

  protected def modifyInternal(as: ArtifactSource, pmi: ParameterValues): ModificationAttempt
}

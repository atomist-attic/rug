package com.atomist.project.edit

import com.atomist.rug.kind.core.ChangeLogEntry
import com.atomist.source.ArtifactSource

/**
  * Supertrait for attempts to modify a project.
  */
sealed trait ModificationAttempt

/**
  * Project was modified successfully.
  *
  * @param result resulting ArtifactSource containing the changes.
  * There must have been changed files. Otherwise,
  * NoModificationNeeded or FailedModificationAttempt should have been returned.
  */
// TODO list files changed? - This could be more efficient in many cases
// Although this makes compounding harder. We could include Deltas as well.
// We could also add methods to make delta history must more efficient in ArtifactSource e.g. on + etc.
case class SuccessfulModification(
                                   result: ArtifactSource,
                                   changeLogEntries: Seq[ChangeLogEntry[ArtifactSource]] = Nil
                                 )
  extends ModificationAttempt

/**
  * Returned when no modification is needed, because the post condition
  * of the editor is already met.
  *
  * @param comment a comment
  */
case class NoModificationNeeded(comment: String) extends ModificationAttempt

/**
  * Project modification failed.
  *
  * @param failureExplanation description of what went wrong
  */
case class FailedModificationAttempt(failureExplanation: String,
                                     cause: Option[Throwable] = None) extends ModificationAttempt

/**
  * Indicates whether this can be applied to a particular ArtifactSource.
  *
  * @param canApply can we apply this editor to the given ArtifactSource?
  * @param message why we can or can't apply the given editor
  */
case class Applicability(canApply: Boolean, message: String) {

}

object Applicability {

  val OK = Applicability(canApply = true, "OK")
}


package com.atomist.project.edit

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
  * @param impacts the detailed impacts of the changes
  * @param comment comments on what was done
  */
// TODO list files changed? - This could be more efficient in many cases
// Although this makes compounding harder. We could include Deltas as well.
// We could also add methods to make delta history must more efficient in ArtifactSource e.g. on + etc.
case class SuccessfulModification(
                                   result: ArtifactSource,
                                   impacts: Set[Impact],
                                   comment: String
                                 ) extends ModificationAttempt

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
case class FailedModificationAttempt(failureExplanation: String) extends ModificationAttempt

/**
  * Indicates whether this can be applied to a particular ArtifactSource.
  *
  * @param canApply can we apply this editor to the given ArtifactSource?
  * @param message why we can or can't apply the given editor
  */
case class Applicability(canApply: Boolean, message: String) {

  /**
    * Are both these Applicability objects applicable?
    */
  def and(that: Applicability) = Applicability(canApply && that.canApply, message + "/" + that.message)

  def &&(that: Applicability) = this and that
}

object Applicability {

  val OK = Applicability(canApply = true, "OK")
}

/**
  * Tag supertrait for objects representing an impact of a code change.
  */
sealed trait Impact

object ContractImpact extends Impact
object CodeImpact extends Impact
object ConfigImpact extends Impact
object TestsImpact extends Impact
object CommentsImpact extends Impact
object ReadmeImpact extends Impact
object DependenciesImpact extends Impact

object Impacts {

  val UnknownImpacts = Set(ContractImpact, CodeImpact, ConfigImpact, TestsImpact, CommentsImpact, ReadmeImpact, DependenciesImpact)
}

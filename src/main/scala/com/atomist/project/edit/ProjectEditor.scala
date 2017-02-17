package com.atomist.project.edit

import com.atomist.param.ParameterValues
import com.atomist.project.common.MissingParametersException
import com.atomist.project.ProjectDelta
import com.atomist.source.ArtifactSource

/**
  * Interface implemented by classes that know how to modify an existing project.
  * Extend ProjectEditorSupport rather than implementing this interface directly.
  */
trait ProjectEditor extends ProjectDelta {

  /**
    * Attempt to create a new ArtifactSource based on applying this editor to the given one.
    *
    * @param as existing sources
    * @param poa data for modification
    * @return object indicating whether successful modification was possible.
    *         Returns a FailedModificationAttempt if it's impossible to make any change,
    *         because of error or because the codebase was simply ineligible.
    *         Return NoModificationNeeded if the codebase already satisfies the invariants
    *         of the operation, or if it's not critical that the change was made
    *         (e.g. adding comments files that meet a certain criteria).
    * @throws MissingParametersException if any parameters are missing
    */
  @throws[MissingParametersException]
  def modify(as: ArtifactSource, poa: ParameterValues): ModificationAttempt

  /**
    * Is this editor potentially applicable to the given ArtifactSource?
    * This may not be the same as it is *actually* applicable, because
    * some editors may apply to such projects but not presently be applicable
    * because of a conflict etc. The editor *will* be applicable if
    * it could be applied in theory, but is unnecessary because the post condition
    * of the editor is already satisfied.
    *
    * @param as an ArtifactSource
    * @return the applicability
    */
  def potentialApplicability(as: ArtifactSource): Applicability = applicability(as)

  /**
    * Is this editor applicable to the given ArtifactSource?
    *
    * @param as an ArtifactSource
    * @return the applicability
    */
  def applicability(as: ArtifactSource): Applicability

  /**
    * Does the ArtifactSource already meet the post condition?
    * Subclasses can choose to override the default return of false.
    */
  def meetsPostcondition(as: ArtifactSource): Boolean = false

  /**
    * Return the reverse of this ProjectEditor, or None if not found
    *
    * @return an Optional
    */
  def reverse: Option[ProjectEditor] = None
}

package com.atomist.project.edit.common

import com.atomist.param.{Parameter, ParameterValue, ParameterValues, Tag}
import com.atomist.project.ProjectOperationArguments
import com.atomist.project.edit._
import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging

/**
  * Applies multiple editors in the course of a larger goal:
  * E.g. Edit POM, create new Java file.
  */
class CompoundProjectEditor(
                             val name: String,
                             val description: String,
                             val components: Seq[ProjectEditor])
  extends ProjectEditor with LazyLogging {

  private val _components: Seq[ProjectEditor] = components

  /**
    * Return this editor with a new name.
    */
  def as(newName: String): CompoundProjectEditor = {
    new CompoundProjectEditor(newName, description, components)
  }

  /**
    * Return this editor with a new description.
    */
  def describedAs(newDescription: String): CompoundProjectEditor = {
    new CompoundProjectEditor(name, newDescription, components)
  }

  /**
    * Potential impact is the maximum of all impacts
    */
  override val impacts: Set[Impact] =
  _components.flatMap(c => c.impacts).toSet

  override def applicability(as: ArtifactSource): Applicability = {
    val vetoers = _components.filter(e => !e.applicability(as).canApply)
    Applicability(
      vetoers.isEmpty,
      vetoers.map(v => v.applicability(as).message).mkString("/")
    )
  }

  /**
    * Attempt to modify the sources.
    *
    * @param as existing sources
    * @param pmi data for modification
    * @return object indicating whether successful modification was possible
    */
  override def modify(as: ArtifactSource, pmi: ProjectOperationArguments): ModificationAttempt = {
    if (_components.isEmpty)
      return FailedModificationAttempt(s"No ProjectEditors in $this")

    var result = as
    var remainingEditors = _components
    var failure: Option[FailedModificationAttempt] = None
    var impacts: Set[Impact] = Set()
    while (remainingEditors.nonEmpty && failure.isEmpty) {
      val editor = remainingEditors.head
      if (!editor.applicability(result).canApply)
        failure = Some(FailedModificationAttempt(s"Can't apply $editor to ${as.getIdString}"))
      else {
        remainingEditors = remainingEditors.tail
        editor.modify(result, pmi) match {
          case sm: SuccessfulModification =>
            logger.debug(s"Editor $editor succeeded: $sm")
            result = sm.result
            impacts = impacts ++ sm.impacts
          case nm: NoModificationNeeded =>
            logger.debug(s"Editor $editor didn't change anything")
          case fm: FailedModificationAttempt =>
            logger.warn(s"Editor $editor failed: $fm")
            failure = Some(fm)
        }
      }
    }
    failure getOrElse SuccessfulModification(result, impacts, "OK")
  }

  /**
    * Custom keys for this template. Must be satisfied in ParameterValues passed in.
    */
  override val parameters: Seq[Parameter] =
  _components.flatMap(e => e.parameters) // Can now have duplicate names
    .groupBy(p => p.getName)
    .values // A number of lists, each unique
    .map(v => v.head) // Take the first of each
    .toSeq

  override val tags: Seq[Tag] =
    _components.flatMap(e => e.tags) // Can now have duplicate names
      .groupBy(t => t.name)
      .values // A number of lists, each unique
      .map(v => v.head) // Take the first of each
      .toSeq

  override def toString: String =
    s"${getClass.getSimpleName}:$name;${components.size} components;descr=$description"
}

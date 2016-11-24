package com.atomist.util.template

import java.util.{List => JList}

import com.atomist.project.ProjectOperation

import scala.collection.JavaConversions._

@deprecated
object TemplateValidationResult {

  def apply(name: String, error: TemplateError) =
    new TemplateValidationResult(None, Seq(error), Nil)

  implicit def stringToTemplateError(s: String): TemplateError = TemplateError(s)
}

@deprecated
trait ValidationResult {

  def errors: JList[TemplateError]

  def warnings: JList[TemplateError]

  def isValid = errors.isEmpty

  /**
    * Is the template free of both errors and warnings?
    */
  def isClean = isValid && warnings.isEmpty
}

/**
  * Errors are definite mistakes in the template. Warnings may or may not
  * mean problems: for example, the use of $ other.
  */
@deprecated("Copied from generator-lib and used by rug-botlet-runner. Is this still valid?")
case class TemplateValidationResult(
                                     projectOperation: Option[ProjectOperation],
                                     errors: JList[TemplateError],
                                     warnings: JList[TemplateError]
                                   )
  extends ValidationResult

/**
  * Information about a template error.
  */
@deprecated
case class TemplateError(
                          msg: String,
                          templateName: Option[String] = None,
                          line: Option[Int] = None,
                          column: Option[Int] = None
                        )

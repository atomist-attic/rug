package com.atomist.project.common.support

import _root_.java.time.LocalDate
import _root_.java.time.format.DateTimeFormatter

import com.atomist.param.ParameterValidationPatterns.{GroupName, ProjectName}
import com.atomist.param.{Parameter, ParameterValidationPatterns, ParameterValues}
import com.atomist.project.ProjectOperationArguments
import com.atomist.project.common.support.CommonParameters._
import com.atomist.project.generate.ProjectGenerator

trait CommonParametersProjectOperation extends ProjectOperationParameterSupport {

  private val formatter = DateTimeFormatter.ofPattern("dd MM yyyy ")

  @ComputedParameter("date_time")
  def dateTime(pd: ParameterValues) = LocalDate.now.format(formatter)
}

/**
  * ProjectTemplate with parameter management and common parameters
  * and convenience extractor methods to add type safety to subclasses
  */
trait CommonParametersProjectGenerator extends ProjectGenerator with CommonParametersProjectOperation {

  /**
    * Subclasses can override this.
    *
    * @return default version for new artifacts
    */
  def defaultVersion: String = DefaultVersion

  addParameter(Parameter(GroupId, GroupName).describedAs("Name of your development group"))

  def groupId(pd: ProjectOperationArguments) = pd.paramValue(GroupId)

  addParameter(Parameter(Name, ProjectName).describedAs("Name for the new project"))

  def name(pd: ParameterValues): String = pd.stringParamValue(Name)

  addParameter(Parameter(ArtifactId, ParameterValidationPatterns.ArtifactId)
    .describedAs("Artifact id (optional)")
    .setRequired(false)
    .setDefaultRef("name"))

  def artifactId(pd: ProjectOperationArguments): String = pd.stringParamValue(ArtifactId)

  addParameter(Parameter(Version, ParameterValidationPatterns.Version)
    .describedAs("Version (optional)")
    .setRequired(false)
    .setDefaultValue(defaultVersion))

  def version(pd: ProjectOperationArguments): String = pd.stringParamValue(Version)
}

/**
  * Parameters for creating a new project.
  */
object CommonParameters {

  val GroupId = "group_id"
  val Name = "name"
  val ArtifactId = "artifact_id"
  val Version = "version"
  val DefaultVersion = "0.0.1"
}
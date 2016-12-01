package com.atomist.project

import com.atomist.tree.content.project.ResourceSpecifier
import com.atomist.param.{Parameter, ParameterValue, ParameterValues, Tag}

import scala.beans.BeanProperty

case class SimpleProjectOperationInfo(
                                       @BeanProperty name: String,
                                       @BeanProperty description: String,
                                       override val group: Option[String],
                                       override val version: Option[String],
                                       @BeanProperty tags: Seq[Tag],
                                       @BeanProperty parameters: Seq[Parameter])
  extends ProjectOperationInfo {

  def this(poi: ProjectOperationInfo) =
    this(poi.name, poi.description, poi.group, poi.version, poi.tags, poi.parameters)

  // For Jackson and other bean-oriented mappers
  def getGroup: String = group.orNull

  def geVersion: String = version.orNull

  def getGav: ResourceSpecifier = gav.orNull
}

/**
  * Tag trait for ProjectOperation that creates or modifies a project.
  */
trait ProjectDelta extends ProjectOperation

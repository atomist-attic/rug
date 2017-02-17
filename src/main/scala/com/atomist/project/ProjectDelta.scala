package com.atomist.project

import com.atomist.param.{Parameter, Tag}
import com.atomist.rug.runtime.ParameterizedRug

import scala.beans.BeanProperty

case class SimpleParameterizedRug(
                                       @BeanProperty name: String,
                                       @BeanProperty description: String,
                                       @BeanProperty tags: Seq[Tag],
                                       @BeanProperty parameters: Seq[Parameter])
  extends ParameterizedRug {

  def this(poi: ParameterizedRug) =
    this(poi.name, poi.description, poi.tags, poi.parameters)
}

/**
  * Tag trait for ProjectOperation that creates or modifies a project.
  */
trait ProjectDelta extends ProjectOperation

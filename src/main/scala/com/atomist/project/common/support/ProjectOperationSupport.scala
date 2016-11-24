package com.atomist.project.common.support

import com.atomist.param.{ParameterValue, ParameterValues, Tag}
import com.atomist.project.ProjectOperation

/**
  * Adds convenient tag and parameter computation support.
  */
trait ProjectOperationSupport extends ProjectOperation {

  private var _tags: Seq[Tag] = Nil

  private var _parameterComputers: Seq[ParameterComputer] = Nil

  protected def addTag(tag: Tag): Unit = _tags = _tags :+ tag

  protected def addTags(ptags: Seq[Tag]): Unit = _tags = _tags ++ ptags

  final override def tags: Seq[Tag] = _tags

  def addParameterComputer(pc: ParameterComputer): Unit = this._parameterComputers = _parameterComputers :+ pc

  // TODO should be able to see previously computed ones
  override final def computedParameters(pvs: ParameterValues): Seq[ParameterValue] =
    _parameterComputers.flatMap(pc => pc.computedParameters(this, pvs))
}

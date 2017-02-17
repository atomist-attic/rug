package com.atomist.project.common.support

import com.atomist.param.{ParameterizedSupport, Tag}
import com.atomist.project.ProjectOperation

/**
  * Adds convenient tag and parameter computation support.
  */
trait ProjectOperationSupport
  extends ProjectOperation
   with ParameterizedSupport{

  private var _tags: Seq[Tag] = Nil

  protected def addTag(tag: Tag): Unit = _tags = _tags :+ tag

  protected def addTags(ptags: Seq[Tag]): Unit = _tags = _tags ++ ptags

  final override def tags: Seq[Tag] = _tags

}

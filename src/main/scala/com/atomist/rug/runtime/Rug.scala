package com.atomist.rug.runtime

import com.atomist.param.Tag

/**
  * For things common to _all_ Rugs
  */
trait Rug {

  def name: String

  def description: String

  def tags: Seq[Tag]
}

package com.atomist.rug.runtime

import com.atomist.param.{Parameter, Tag}

/**
  * Common stuff for Handlers
  */
trait Handler {

  def name: String

  def tags: Seq[Tag]

  def description: String
}

trait ParameterizedHandler extends Handler {
  def parameters: Seq[Parameter]
}
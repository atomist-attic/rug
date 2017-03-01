package com.atomist.project.common.template

import com.atomist.rug.runtime.{ParameterizedRug, RugSupport}

trait TemplateBasedProjectOperationInfo
  extends ParameterizedRug
  with RugSupport{

  def templateType: Option[String]
}

class InvalidTemplateException(msg: String, t: Throwable = null) extends Exception(msg, t)

package com.atomist.project.common.template

import com.atomist.rug.runtime.ParameterizedRug

trait TemplateBasedProjectOperationInfo extends ParameterizedRug {

  def templateType: Option[String]
}

class InvalidTemplateException(msg: String, t: Throwable = null) extends Exception(msg, t)
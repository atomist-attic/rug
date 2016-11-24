package com.atomist.project.common.template

import com.atomist.project.ProjectOperationInfo

trait TemplateBasedProjectOperationInfo extends ProjectOperationInfo {

  def templateType: Option[String]
}

class InvalidTemplateException(msg: String, t: Throwable = null) extends Exception(msg, t)
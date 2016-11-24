package com.atomist.project.common.script

import com.atomist.param.ParameterValue
import com.atomist.project.ProjectOperationArguments

object NashornProjectScriptUtils {

  def toJavaScriptMapParameter(pd: ProjectOperationArguments): Map[String, Object] = {
    if (pd == null) Map[String, Object]()
    else toJavaScriptMapParameter(pd.parameterValues)
  }

  def toJavaScriptMapParameter(pvs: Seq[ParameterValue]): Map[String, Object] = {
    val parameterMap: Map[String, Object] = pvs.map(pv => (pv.getName, pv.getValue)).toMap
    parameterMap
  }
}

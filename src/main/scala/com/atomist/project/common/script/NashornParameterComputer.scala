package com.atomist.project.common.script

import java.util.{Map => JMap}
import javax.script.ScriptException

import com.atomist.param.{ParameterValue, ParameterValues, Parameterized, SimpleParameterValue}
import com.atomist.project.common.ReportingUtils
import com.atomist.util.script.nashorn.AbstractNashornScriptBacked
import com.atomist.util.script.{InvalidScriptException, Script}

import scala.collection.JavaConverters._

/**
  * Compute parameters using JavaScript functions in a given script file.
  */
class NashornParameterComputer(script: Script)
  extends AbstractNashornScriptBacked(Seq(script))
    with ScriptBackedParameterComputer {

  import ScriptFunctions._

  override protected def globalTypeDeclarations: String = NashornParameterComputer.GlobalTypeDeclarations

  override def computedParameters(op: Parameterized, pvs: ParameterValues): Seq[ParameterValue] = {
    val parameterMap: Map[String, Object] =
      if (pvs == null) Map[String, Object]() else NashornProjectScriptUtils.toJavaScriptMapParameter(pvs.parameterValues)

    try {
      val r = invocable.invokeFunction(ComputedParametersFunction, parameterMap.asJava)
      r match {
        case m: JMap[String @unchecked, String @unchecked] =>
          m.asScala.toList.map(tup => SimpleParameterValue(tup._1, tup._2))
        case null =>
          // Treat null as empty map
          Nil
        case wtf =>
          val bad = s"Unexpected return type $wtf of type ${wtf.getClass} evaluating script:\n${ReportingUtils.withLineNumbers(js)}"
          throw new InvalidScriptException(bad, null)
      }
    }
    catch {
      case nme: NoSuchMethodException =>
        val bad = s"Did not find required method '$ComputedParametersFunction' in script:\n${ReportingUtils.withLineNumbers(js)}"
        throw new InvalidScriptException(bad, null)
      case sex: ScriptException =>
        val bad = s"Error evaluating script:\n${ReportingUtils.withLineNumbers(js)}: ${sex.getMessage}"
        throw new InvalidScriptException(bad, sex)
    }
  }
}

object NashornParameterComputer {

  val DefaultFileName = "params.js"

  val GlobalTypeDeclarations =
    """
      |// No global type declarations needed
      |
    """.stripMargin
}
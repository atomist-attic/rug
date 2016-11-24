package com.atomist.rug.runtime.lang.js

import java.util.concurrent.ConcurrentHashMap
import java.util.{Map => JMap}

import com.atomist.rug.runtime.FunctionInvocationContext
import com.atomist.util.script.Script
import com.atomist.util.script.nashorn.AbstractNashornScriptBacked
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions._

/**
  * Fronts a local JavaScript expression
  *
  * @param script
  * @param functionName
  */
class NashornExpressionEngine private(
                                       script: Script,
                                       functionName: String
                                     )
  extends AbstractNashornScriptBacked(Seq(script)) {

  override protected def globalTypeDeclarations: String = ""

  def evaluate(context: Object, alias: String, identifierMap: Map[String, Object]): Object = {
    val parameterMap: JMap[String, Object] = identifierMap

    val argsToUse: Seq[Object] =
      Seq(context) ++ parameterMap.values ++ Seq(parameterMap)

    val r = invocable.invokeFunction(functionName, argsToUse: _*)
    r
  }

  def evaluate(ic: FunctionInvocationContext[_]): Object = {
    val parameterMap: JMap[String, Object] = ic.identifierMap

    val localArgs = if (ic.localArgs != null) ic.localArgs else Nil
    val argsToUse: Seq[Object] =
      Seq(parameterMap, ic.target.asInstanceOf[Object], ic) ++
        localArgs ++
        ic.identifierMap.values

    val r = invocable.invokeFunction(functionName, argsToUse: _*)
    r
  }
}

object NashornExpressionEngine extends LazyLogging {

  import scala.collection.JavaConverters._

  private val invocables: scala.collection.mutable.Map[String, NashornExpressionEngine] =
    new ConcurrentHashMap[String, NashornExpressionEngine]().asScala

  private def cacheKey(js: String, functionName: String) = js + "_" + functionName

  /** Return invocable. May be cached */
  private def getInvocable(js: String, functionName: String): NashornExpressionEngine = {
    val k = cacheKey(js, functionName)
    val nee: NashornExpressionEngine = invocables.get(k) match {
      case Some(nee) =>
        logger.debug(s"JavaScript Cache hit!\n$k")
        nee
      case None =>
        logger.debug(s"JavaScript Cache miss!\n$k")
        val created = new NashornExpressionEngine(Script(functionName, js), functionName)
        invocables.put(k, created)
        created
    }
    nee
  }

  def evaluator(context: Object, alias: String, identifierMap: Map[String, Object], expr: String): NashornExpressionEngine = {
    val (functionName: String, addReturn: Boolean, params: String) = prepareJavaScriptFunction(identifierMap, expr)

    val js =
      s"""
         |function $functionName($alias $params) {
         |   $expr; ${if (addReturn) "return" else ""}
         |}
      """.stripMargin
    logger.debug(s"Executing\n$js\n")

    getInvocable(js, functionName)
  }

  def evaluator[T](ic: FunctionInvocationContext[T], expr: String): NashornExpressionEngine = {
    val identifierMap = ic.identifierMap

    val (functionName: String, addReturn: Boolean, params: String) = prepareJavaScriptFunction(identifierMap, expr)

    val js =
      s"""
         |function $functionName(params, ${ic.targetAlias}, ic $params) {
         |   ${if (addReturn) "return" else ""} $expr;
         |}
      """.stripMargin

    logger.debug(s"Executing\n$js\n")
    getInvocable(js, functionName)
  }

  private def prepareJavaScriptFunction(identifierMap: Map[String, Object], expr: String, entryCharacter: String = ","): (String, Boolean, String) = {
    val functionName = "rug_engine_synthetic_function"
    val addReturn = !(expr.contains("{") || expr.contains("return"))

    // May not need this if we restrict parameters
    val paramNamesAsValidJSIdentifiers = identifierMap.keys.map(s => s.replace("-", "_"))

    val params = if (identifierMap.isEmpty) ""
    else entryCharacter + paramNamesAsValidJSIdentifiers.mkString(",")
    (functionName, addReturn, params)
  }
}
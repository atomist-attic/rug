package com.atomist.rug.runtime.lang

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.parser.ScriptBlockAction
import com.atomist.rug.runtime.lang.js.NashornExpressionEngine
import com.atomist.scalaparsing.JavaScriptBlock

/**
  * Default support for executing scripts in Clojure and JavaScript.
  */
object DefaultScriptBlockActionExecutor extends ScriptBlockActionExecutor {

  override def execute(sba: ScriptBlockAction,
                       mv: Object,
                       alias: String,
                       identifierMap: Map[String, Object]): Object = {
    sba.scriptBlock match {
      case jsb: JavaScriptBlock =>
        executeJavaScript(mv, alias, identifierMap, jsb)
      case wtf => throw new RugRuntimeException(null, s"Unknown script block type in $wtf")
    }
  }

  private def executeJavaScript(mv: Object, alias: String, identifierMap: Map[String, Object], jsb: JavaScriptBlock): AnyRef = {
    val evaluator = NashornExpressionEngine.evaluator(mv, alias, identifierMap, jsb.content)
    evaluator.evaluate(mv, alias, identifierMap)
  }
}

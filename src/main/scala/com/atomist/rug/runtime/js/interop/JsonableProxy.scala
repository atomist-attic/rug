package com.atomist.rug.runtime.js.interop

import com.atomist.util.lang.JavaScriptArray
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Consistent JSON-based toString stringification
  */
private[interop] trait JsonableProxy {

  override def toString: String = {
    val typeKey = s""""__backingObject__": "${getClass.getSimpleName}#$hashCode""""
    "{" + (propsToJsonPairs :+ typeKey).mkString(", ") + "}"
  }

  protected def propertyMap: Map[String,Any]

  import scala.collection.JavaConverters._

  private def propsToJsonPairs: Seq[String] =
    (propertyMap map {
      case (k, som: ScriptObjectMirror) =>
        s""""$k": ${som.eval("JSON.stringify(this)")}"""
      case (k, js: JavaScriptArray[_]) =>
        s""""$k": [${js.lyst.asScala.mkString(", ")}]"""
      case (k, v) =>
        s""""$k": "$v""""
    }).toSeq

}

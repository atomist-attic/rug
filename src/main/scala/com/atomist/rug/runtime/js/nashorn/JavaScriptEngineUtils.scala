package com.atomist.rug.runtime.js.nashorn

import com.atomist.rug.runtime.js.JavaScriptObject
import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.ConsString

import scala.collection.JavaConverters._

/**
  * Some useful stuff
  */
object JavaScriptEngineUtils {
    def toJavaType(o: AnyRef): Object = o match {
      case s: ConsString => s.toString
      case r: ScriptObjectMirror if r.isArray =>
        r.values().asScala
      case r: JavaScriptObject if r.isSeq =>
        r.values()
      case x => x
    }
}

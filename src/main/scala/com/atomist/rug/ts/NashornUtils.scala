package com.atomist.rug.ts

import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.ConsString

/**
  * Utilities to help in binding to Nashorn
  */
object NashornUtils {

  import scala.collection.JavaConverters._

  def extractProperties(som: ScriptObjectMirror): Map[String, Object] =
    som.entrySet().asScala.map(me => me.getKey -> me.getValue).toMap

  def toJavaType(nashornReturn: Object) = nashornReturn match {
    case s: ConsString => s.toString
    case x => x
  }
}

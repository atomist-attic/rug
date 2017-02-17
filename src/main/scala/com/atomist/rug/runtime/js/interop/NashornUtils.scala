package com.atomist.rug.runtime.js.interop

import java.util.Map.Entry
import java.util.Objects

import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.ConsString

/**
  * Utilities to help in binding to Nashorn
  */
object NashornUtils {

  import scala.collection.JavaConverters._

  def extractProperties(som: ScriptObjectMirror): Map[String, Object] =
    som.entrySet().asScala.map(me => me.getKey -> me.getValue).toMap

  def toJavaType(nashornReturn: Object): Object = nashornReturn match {
    case s: ConsString => s.toString
    case r: ScriptObjectMirror if r.isArray =>
      r.values().asScala
    case x => x
  }

  def toJavaMap(nashornReturn: Object): Map[String, Object] =
    nashornReturn match {
      case som: ScriptObjectMirror =>
        val scalaMap = som.entrySet().asScala.map{
          case e: Entry[String, Object] =>
            (e.getKey, e.getValue)
        }.toMap
          scalaMap.mapValues {
            case som: ScriptObjectMirror =>
              toJavaMap(som)
            case x =>
              toJavaType(x)

        }
    }

  def toScalaSeq(nashornReturn: Object): Seq[Object] = nashornReturn match {
    case r: ScriptObjectMirror if r.isArray =>
      r.values().asScala.toSeq
  }

  /**
    * Return the given property of the JavaScript object or null if not found
    */
  def stringProperty(som: ScriptObjectMirror, name: String): String = {
    som.get(name) match {
      case null => null
      case x => Objects.toString(x)
    }
  }

  /**
    * Call the given JavaScript function, which must return a string
    */
  def stringFunction(som: ScriptObjectMirror, name: String): String = {
    som.callMember(name) match {
      case null => null
      case x => Objects.toString(x)
    }
  }

  /**
    * Are all these properties defined
    */
  def hasDefinedProperties(som: ScriptObjectMirror, properties: String*): Boolean =
    properties.forall(p => som.get(p) != null)
}

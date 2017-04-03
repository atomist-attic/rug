package com.atomist.rug.runtime.js.interop

import java.util.Map.Entry
import java.util.Objects

import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.ConsString
import scala.util.control.Exception._

/**
  * Utilities to help in binding to Nashorn.
  */
object NashornUtils {

  import scala.collection.JavaConverters._

  def extractProperties(som: ScriptObjectMirror): Map[String, Object] =
    som.entrySet().asScala
      .map(me => me.getKey -> me.getValue)
      .filter(_._2 match {
        case som: ScriptObjectMirror if som.isFunction => false
        case _ => true
      })
      .toMap

  /**
    * Return the current state of no-arg methods on this object
    */
  def extractNoArgFunctions(som: ScriptObjectMirror): Map[String, Object] = {
    som.entrySet().asScala
      .map(me => me.getKey -> me.getValue)
      .flatMap {
        case (key, f: ScriptObjectMirror) if isNoArgFunction(f) =>
          // If calling the function throws an exception, discard the value.
          // This will happen with builder stubs that haven't been fully initialized
          // Otherwise, use it
          allCatch.opt(som.callMember(key))
            .map(result => (key, result))
        case _ => None
      }.toMap
  }

  // TODO this is fragile but can't find a Nashorn method to do it
  private def isNoArgFunction(f: ScriptObjectMirror): Boolean = {
    f.isFunction && {
      val s = f.toString
      //println(s)
      s.startsWith("function ()")
    }
  }

  def toJavaType(nashornReturn: Object): Object = nashornReturn match {
    case s: ConsString => s.toString
    case r: ScriptObjectMirror if r.isArray =>
      r.values().asScala
    case x => x
  }

  def toJavaMap(nashornReturn: Object): Map[String, Object] =
    nashornReturn match {
      case som: ScriptObjectMirror =>
        val scalaMap = som.entrySet().asScala.map {
          e: Entry[String, Object] => (e.getKey, e.getValue)
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
    * Return the given property of the JavaScript object or default value if not found
    *
    * @param default default value if not found. Defaults to null.
    */
  def stringProperty(som: ScriptObjectMirror, name: String, default: String = null): String =
    som.get(name) match {
      case null => default
      case x => Objects.toString(x)
    }

  /**
    * Call the given JavaScript function, which must return a string
    */
  def stringFunction(som: ScriptObjectMirror, name: String): String =
    som.callMember(name) match {
      case null => null
      case x => Objects.toString(x)
    }

  /**
    * Are all these properties defined
    */
  def hasDefinedProperties(som: ScriptObjectMirror, properties: String*): Boolean =
    properties.forall(p => som.get(p) != null)
}

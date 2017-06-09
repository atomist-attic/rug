package com.atomist.rug.runtime.js

import java.util.Objects

import scala.util.control.Exception.allCatch

/**
  * Talk to objects in a JavaScriptEngine
  */
trait JavaScriptObject {
  def hasMember(name: String): Boolean

  def getMember(name: String): AnyRef

  def setMember(name: String, value: AnyRef): Unit

  def callMember(name: String, args: AnyRef*): AnyRef

  def call(thisArg: AnyRef, args: AnyRef*): AnyRef

  def isEmpty: Boolean

  def values(): Seq[AnyRef]

  def isSeq: Boolean

  def isMap: Boolean

  def isFunction: Boolean

  def eval(js: String): AnyRef

  def entries(): Map[String, AnyRef]

  def keys(all: Boolean = true): Iterable[String] = {
    entries().keys
  }

  /**
    * Get all key values, ignoring exceptions, which can be thrown by Nashorn entrySet.
    */
  private def safeObjectMap(): Map[String, AnyRef] =
    keys()
      .flatMap(key => {
        // TypeScript "get" properties can throw exceptions
        val maybeValue = allCatch.opt(getMember(key))
        maybeValue.map(value => key -> value)
      }).toMap

  def extractProperties(): Map[String, AnyRef] =
    safeObjectMap flatMap {
      case (_, som: JavaScriptObject) if som.isFunction =>
        None
      case (key, value) =>
        Some(key -> value)
    }

  // TODO this is fragile but can't find a Nashorn method to do it
  private def isNoArgFunction(f: JavaScriptObject): Boolean = {
    f.isFunction && {
      val s = f.toString
      s.startsWith("function ()")
    }
  }

  /**
    * Return the current state of no-arg methods on this object
    */
  def extractNoArgFunctionValues(): Map[String, AnyRef] = {
    val m = safeObjectMap().flatMap {
      case (key, f: JavaScriptObject) if isNoArgFunction(f) =>
        // If calling the function throws an exception, discard the value.
        // This will happen with builder stubs that haven't been fully initialized
        // Otherwise, use it
        allCatch.opt(callMember(key))
          .map(result => {
            (key, result)
          })
      case _ => None
    }
    m
  }

  /**
    * Return the given property of the JavaScript object or default value if not found
    *
    * @param default default value if not found. Defaults to null.
    */
  def stringProperty(name: String, default: String = null): String =
    getMember(name) match {
      case null => default
      case x => Objects.toString(x)
    }

  /**
    * Call the given JavaScript function, which must return a string
    */
  def stringFunction(name: String): String =
    callMember(name) match {
      case null => null
      case x => Objects.toString(x)
    }

  /**
    * Are all these properties defined
    */
  def hasDefinedProperties(properties: String*): Boolean =
    properties.forall(p => getMember(p) != null)
}

case class UNDEFINED()

package com.atomist.rug.runtime.js

import com.atomist.project.archive.RugResolver
import com.atomist.rug.runtime.Rug
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._

/**
  * Interface for finding rugs of different types in Nashorn
  */
trait JavaScriptRugFinder[R <: Rug] {

  def find(jsc: JavaScriptContext, resolver: Option[RugResolver] = None): Seq[R] = {
    jsc.vars.flatMap {
      case Var(_, handler) if isValidRug(handler) =>
        create(jsc, handler, resolver)
      case Var(_, arr) if arr.isArray =>
        arr.values().asScala.collect {
          case som: ScriptObjectMirror if isValidRug(som) => som
        }.flatMap(create(jsc, _, resolver))
      case _ => None
    }
  }

  /**
    * Is the supplied thing valid?
    */
  def isValidRug(obj: ScriptObjectMirror): Boolean = {
    obj.hasMember("__kind") &&
      obj.hasMember("__description") &&
      (obj.hasMember("__name") || obj.hasMember("constructor")) &&
      isValid(obj)
  }

  def isValid(obj: ScriptObjectMirror): Boolean

  def create(jsc: JavaScriptContext, jsVar: ScriptObjectMirror, resolver: Option[RugResolver]): Option[R]
}

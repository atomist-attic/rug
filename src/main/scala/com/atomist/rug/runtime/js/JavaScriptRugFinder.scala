package com.atomist.rug.runtime.js

import com.atomist.project.archive.RugResolver
import com.atomist.rug.runtime.Rug

/**
  * Interface for finding rugs of different types in Nashorn
  */
trait JavaScriptRugFinder[R <: Rug] {

  def find(jsc: JavaScriptEngineContext, resolver: Option[RugResolver] = None): Seq[R] = {
    jsc.members().flatMap {
      case JavaScriptMember(_, handler) if isValidRug(handler) =>
        create(jsc, handler, resolver)
      case JavaScriptMember(_, arr) if arr.isSeq =>
        arr.values().collect {
          case som: JavaScriptObject if isValidRug(som) => som
        }.flatMap(create(jsc, _, resolver))
      case _ => None
    }
  }

  /**
    * Is the supplied thing valid?
    */
  def isValidRug(obj: JavaScriptObject): Boolean = {
    obj.hasMember("__kind") &&
      obj.hasMember("__description") &&
      (obj.hasMember("__name") || obj.hasMember("constructor")) &&
      isValid(obj)
  }

  def isValid(obj: JavaScriptObject): Boolean

  def create(jsc: JavaScriptEngineContext, jsVar: JavaScriptObject, resolver: Option[RugResolver]): Option[R]
}

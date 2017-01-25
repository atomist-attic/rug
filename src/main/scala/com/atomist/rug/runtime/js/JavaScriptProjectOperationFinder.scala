package com.atomist.rug.runtime.js

import com.atomist.project.ProjectOperation

import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Find and instantiate JavaScript editors/generators in a nashorn
  */
abstract class JavaScriptProjectOperationFinder[T <: ProjectOperation]
  extends JavaScriptRugFinder[T]{

  def signatures: Set[JsRugOperationSignature]

  override def find(jsc: JavaScriptContext): Seq[T] = {
    jsc.vars.map(v => (v, matchesSignature(v.scriptObjectMirror))).collect{
      case (v, true) => createProjectOperation(jsc,v.scriptObjectMirror)
    }
  }

  def createProjectOperation(jsc: JavaScriptContext, fnVar: ScriptObjectMirror): T

  /**
    * Does the var match the signature of this ProjectOperation kind?
    * @param obj
    * @return
    */
  private def matchesSignature(obj: ScriptObjectMirror): Boolean = {
    signatures.exists {
      case JsRugOperationSignature(fns, props) =>
        val fnCount = fns.count(fn => {
          obj.hasMember(fn) && obj.getMember(fn).asInstanceOf[ScriptObjectMirror].isFunction
        })
        val propsCount = props.count(prop => {
          obj.hasMember(prop) // TODO make stronger check
        })
        fnCount == fns.size && propsCount == props.size
    }
  }

  case class JsRugOperationSignature(functionsNames: Set[String], propertyNames: Set[String])
}

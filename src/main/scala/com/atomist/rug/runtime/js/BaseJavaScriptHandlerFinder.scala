package com.atomist.rug.runtime.js

import com.atomist.param.{Parameter, Tag}
import com.atomist.project.archive.RugResolver
import com.atomist.rug.runtime.Rug
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Some common things used to extract handlers from nashorn
  */
abstract class BaseJavaScriptHandlerFinder[T <: Rug]
  extends JavaScriptUtils
    with JavaScriptRugFinder[T] {

  override def find(jsc: JavaScriptContext, resolver: Option[RugResolver] = None): Seq[T] = {
    jsc.vars.collect {
      case Var(_, handler) if isValidHandler(handler) && handler.getMember("__kind") == kind => extractHandler(jsc,handler)
    }.flatten
  }

  /**
    *
    * The value of kind in JS
    * @return
    */
  def kind: String

  /**
    * Is the supplied thing valid at all?
    */
  protected def isValidHandler(obj: ScriptObjectMirror): Boolean = {
    obj.hasMember("__kind") && obj.hasMember("__name") && obj.hasMember("__description") && obj.hasMember("handle")
  }

  protected def extractHandler(jsc: JavaScriptContext, handler: ScriptObjectMirror): Option[T]

  protected def name(obj: ScriptObjectMirror): String = obj.getMember("__name").asInstanceOf[String]

  protected def description(obj: ScriptObjectMirror): String = obj.getMember("__description").asInstanceOf[String]

  protected def kind(obj: ScriptObjectMirror): String = obj.getMember("__kind").asInstanceOf[String]

  protected def tags(someVar: ScriptObjectMirror): Seq[Tag] = {
    tags(someVar, Seq("__tags"))
  }

  /**
    * Either read the parameters field or look for annotated parameters
    */
  protected def parameters(someVar: ScriptObjectMirror): Seq[Parameter] = {
    parameters(someVar, Seq("__parameters"))
  }
}

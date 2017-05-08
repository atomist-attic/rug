package com.atomist.rug.runtime.js

import com.atomist.param.{Parameter, Tag}
import com.atomist.project.archive.RugResolver
import com.atomist.rug.BadPlanException
import com.atomist.rug.runtime.Rug
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * Some common things used to extract handlers from nashorn
  */
abstract class BaseJavaScriptHandlerFinder[T <: Rug]
  extends JavaScriptUtils
    with JavaScriptRugFinder[T] {

  override def find(jsc: JavaScriptContext, resolver: Option[RugResolver] = None): Seq[T] = {
    def isHandler(handler: ScriptObjectMirror) =
      isValidHandler(handler) && handler.getMember("__kind") == kind

    jsc.vars.flatMap {
      case Var(_, handler) if isHandler(handler) =>
        extractHandler(jsc, handler)
      case Var(_, arr) if arr.isArray =>
        arr.values().asScala.collect {
          case som: ScriptObjectMirror if isHandler(som) => som
        }
          .flatMap(extractHandler(jsc, _))
      case _ => None
    }
  }

  /**
    *
    * The value of kind in JS
    *
    * @return
    */
  def kind: String

  /**
    * Is the supplied thing valid at all?
    */
  protected def isValidHandler(obj: ScriptObjectMirror): Boolean = {
    obj.hasMember("__kind") && obj.hasMember("__description") && obj.hasMember("handle") &&
      (obj.hasMember("__name") || obj.hasMember("constructor"))
  }

  protected def extractHandler(jsc: JavaScriptContext, handler: ScriptObjectMirror): Option[T]

  protected def name(obj: ScriptObjectMirror): String = {
    if(obj.hasMember("__name")){
      obj.getMember("__name").asInstanceOf[String]
    }else{
      Try(obj.getMember("constructor").asInstanceOf[ScriptObjectMirror].getMember("name").asInstanceOf[String]) match {
        case Success(name) => name
        case Failure(error) => throw new BadPlanException(s"Could not determine name of Rug", error)
      }
    }
  }

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

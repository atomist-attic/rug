package com.atomist.rug.runtime.js

import scala.collection.JavaConverters._
import com.atomist.param.Tag
import com.atomist.rug.runtime.Handler
import com.atomist.rug.runtime.js.interop.JavaScriptHandlerContext
import com.atomist.source.ArtifactSource
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.util.Try

/**
  * Some common things used to extract handlers from nashorn
  */
trait HandlerFinder[T <: Handler] {

  /**
    * Is the supplied thing valid at all?
    * @param obj
    * @return
    */
  protected def isValidHandler(obj: ScriptObjectMirror): Boolean = {
    obj.hasMember("__kind") && obj.hasMember("__name") && obj.hasMember("__description") && obj.hasMember("handle")
  }

  def extractHandlers(rugAs: ArtifactSource, ctx: JavaScriptHandlerContext): Seq[T] = {
    val jsc = new JavaScriptContext(rugAs)
    jsc.vars.collect {
      case Var(_, obj) if isValidHandler(obj) => extractHandler(obj, rugAs, ctx)
    }.flatten
  }

  protected def extractHandler(obj: ScriptObjectMirror, rugAs: ArtifactSource, ctx: JavaScriptHandlerContext): Option[T]

  protected def name(obj: ScriptObjectMirror): String = obj.getMember("__name").asInstanceOf[String]
  protected def description(obj: ScriptObjectMirror): String = obj.getMember("__description").asInstanceOf[String]
  protected def kind(obj: ScriptObjectMirror): String = obj.getMember("__kind").asInstanceOf[String]
  protected def handle(obj: ScriptObjectMirror): ScriptObjectMirror = obj.getMember("handle").asInstanceOf[ScriptObjectMirror]

  protected def tags(someVar: ScriptObjectMirror): Seq[Tag] = {
    Try {
      someVar.getMember("__tags") match {
        case som: ScriptObjectMirror =>
          val stringValues = som.values().asScala collect {
            case s: String => s
          }
          stringValues.map(s => Tag(s, s)).toSeq
        case _ => Nil
      }
    }.getOrElse(Nil)
  }
}


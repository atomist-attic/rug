package com.atomist.rug.runtime.js

import com.atomist.param.Tag
import com.atomist.rug.runtime.js.JavaScriptProjectOperationFinder.JsRugOperationSignature
import com.atomist.rug.runtime.{Handler, SystemEventHandler}
import com.atomist.rug.runtime.js.interop._
import com.atomist.source.ArtifactSource
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Finds and evaluates handlers in a Rug archive.
  */
object JavaScriptHandlerFinder {

  val EventHandlerName = "event-handler"

  val CommandHandlerName = "command-handler"

  val ResponseHandlerName = "response-handler"

  /**
    * Used to recognise JS operations that we can call.
    * TODO - this should probably include type checking too!
    */
//  val KnownSignatures = Map()
//
//    Set(
//    JsRugOperationSignature(EditorType, Set("edit")),
//    JsRugOperationSignature(EditorType, Set("edit"), Set("__name", "__description")),
//    JsRugOperationSignature(ReviewerType,Set("review")),
//    JsRugOperationSignature(ReviewerType,Set("review"), Set("__name", "__description")),
//    JsRugOperationSignature(GeneratorType,Set("populate")),
//    JsRugOperationSignature(GeneratorType,Set("populate"),Set("__name", "__description"))
//  )
  /**
    * Find handler operations in the given Rug archive
    *
    * @param rugAs   archive to look into
    * @return a sequence of instantiated operations backed by JavaScript
    *
    */

  def findEventHandlers(rugAs: ArtifactSource,
                        ctx: JavaScriptHandlerContext): Seq[Handler] = {
    handlersFromVars(rugAs, new JavaScriptContext(rugAs), ctx)
  }

  private def handlersFromVars(rugAs: ArtifactSource, jsc: JavaScriptContext, ctx: JavaScriptHandlerContext): Seq[SystemEventHandler] = {
    jsc.vars.foldLeft(Seq[SystemEventHandler]())((acc: Seq[SystemEventHandler], jsVar) => {
      val obj = jsVar.scriptObjectMirror
      if (obj.hasMember("name") && obj.hasMember("description") && obj.hasMember("handle") && obj.hasMember("expression")) {
        val name = obj.getMember("name").asInstanceOf[String]
        val description = obj.getMember("description").asInstanceOf[String]
        val handle = obj.getMember("handle").asInstanceOf[ScriptObjectMirror]
        val expression: String = obj.getMember("expression") match {
          case x: String => x
          case o: ScriptObjectMirror => o.getMember("expression").asInstanceOf[String]
          case _ => null
        }
        val tags = readTagsFromMetadata(obj)
        acc :+ new JavaScriptEventHandler(expression, handle, obj, rugAs, ctx, name, description, tags)
      } else {
        acc
      }
    })
  }

  protected def readTagsFromMetadata(someVar: ScriptObjectMirror): Seq[Tag] = {
    Try {
      someVar.getMember("tags") match {
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

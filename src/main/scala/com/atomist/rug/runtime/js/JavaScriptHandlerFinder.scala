package com.atomist.rug.runtime.js

import javax.script.SimpleBindings

import com.atomist.event.SystemEventHandler
import com.atomist.param.Tag
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.runtime.js.interop._
import com.atomist.source.ArtifactSource
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Finds and evaluates handlers in a Rug archive.
  */
object JavaScriptHandlerFinder {

  /**
    * Find handler operations in the given Rug archive
    *
    * @param rugAs   archive to look into
    * @param atomist facade to Atomist
    * @return a sequence of instantiated operations backed by JavaScript
    *
    */
  def registerHandlers(rugAs: ArtifactSource,
                       atomist: AtomistFacade,
                       atomistConfig: AtomistConfig = DefaultAtomistConfig): Unit = {

    //TODO - remove this when new Handler model put in
    val bindings = new SimpleBindings()
    bindings.put("atomist", atomist)
    new JavaScriptContext(rugAs,atomistConfig,bindings)
  }

  def fromJavaScriptArchive(rugAs: ArtifactSource,
                            ctx: JavaScriptHandlerContext): Seq[SystemEventHandler] = {

    val jsc = new JavaScriptContext(rugAs)

    handlersFromVars(rugAs, jsc, ctx)
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
        acc :+ new NamedJavaScriptEventHandler(expression, handle, obj, rugAs, ctx, name, description, tags)
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

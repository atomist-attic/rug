package com.atomist.rug.runtime.js

import com.atomist.event.SystemEventHandler
import com.atomist.param.Tag
import com.atomist.plan.TreeMaterializer
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.kind.service.TeamContext
import com.atomist.rug.runtime.js.interop._
import com.atomist.source.ArtifactSource
import com.atomist.tree.pathexpression.PathExpressionEngine
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.util.Try
import scala.collection.JavaConverters._


/**
  * Finds and evaluates handlers in a Rug archive.
  */
object JavaScriptHandlerFinder {

  /**
    * Find and handlers operations in the given Rug archive
    *
    * @param rugAs   archive to look into
    * @param atomist facade to Atomist
    * @return a sequence of instantiated operations backed by JavaScript
    *
    */
  def registerHandlers(rugAs: ArtifactSource,
                       atomist: AtomistFacade,
                       atomistConfig: AtomistConfig = DefaultAtomistConfig): Unit = {
    val jsc = new JavaScriptContext()

    //TODO - remove this when new Handler model put in
    jsc.engine.put("atomist", atomist)
    jsc.load(rugAs)
  }

  def fromJavaScriptArchive(rugAs: ArtifactSource,
                            ctx: JavaScriptHandlerContext,
                            context: JavaScriptContext = null): Seq[SystemEventHandler] = {
    val jsc: JavaScriptContext =
      if (context == null)
        new JavaScriptContext()
      else
        context

    jsc.load(rugAs)
    handlersFromVars(rugAs, jsc, ctx)
  }

  private def handlersFromVars(rugAs: ArtifactSource, jsc: JavaScriptContext, ctx: JavaScriptHandlerContext): Seq[SystemEventHandler] = {
    jsc.vars.foldLeft(Seq[SystemEventHandler]())((acc: Seq[SystemEventHandler], jsVar) => {
      val obj = jsVar.scriptObjectMirror
      val name = obj.getMember("name").asInstanceOf[String]
      val description = obj.getMember("description").asInstanceOf[String]
      val handle = obj.getMember("handle").asInstanceOf[ScriptObjectMirror]
      val expression = obj.getMember("expression") match {
        case x: String => x
        case o: ScriptObjectMirror => o.getMember("expression").asInstanceOf[String]
        case _ => Nil.asInstanceOf[String]
      }

      val tags = readTagsFromMetadata(obj)
      if(name != null && description != null && handle != null && expression != null){
        acc :+ new NamedJavaScriptEventHandler(expression, handle, obj, rugAs, ctx, name, description,tags)
      }else{
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


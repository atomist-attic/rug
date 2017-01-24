package com.atomist.rug.runtime.js.interop

import com.atomist.event.SystemEventHandler
import com.atomist.plan.TreeMaterializer
import com.atomist.rug.kind.service.{MessageBuilder, TeamContext}
import com.atomist.rug.runtime.js.JavaScriptEventHandler
import com.atomist.source.EmptyArtifactSource
import com.atomist.tree.pathexpression.PathExpressionEngine
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.mutable.ListBuffer

/**
  * AtomistFacade for a given team.
  */
class jsModelBackedAtomistFacade(val teamId: String,
                                 val messageBuilder: MessageBuilder,
                                 override val treeMaterializer: TreeMaterializer)
  extends AtomistFacade {

  private val _handlers = ListBuffer.empty[SystemEventHandler]

  private val pexe = new jsPathExpressionEngine(teamContext = this, ee = new PathExpressionEngine)

  // TODO also can take path expression object
  def on(s: String, handler: Any): Unit = {
    handler match {
      case som: ScriptObjectMirror =>
        _handlers.append(new JavaScriptEventHandler(s, som, EmptyArtifactSource(), treeMaterializer, pexe))
    }
  }

  override val registry = Map(
    "PathExpressionEngine" -> pexe
  )

  def handlers: Seq[SystemEventHandler] = _handlers
}

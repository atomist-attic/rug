package com.atomist.rug.runtime.js.interop

import java.util.Collections

import com.atomist.tree.SimpleTerminalTreeNode
import jdk.nashorn.api.scripting.ScriptObjectMirror


/**
  * Context exposed to user JavaScript.
  */
trait UserModelContext {

  def registry: Map[String, Object]

}


/**
  * Entry point to Atomist system
  */
trait AtomistFacade extends UserModelContext {

  def on(s: String, handler: Any): Unit

}


object DefaultAtomistFacade extends AtomistFacade {

  def on(s: String, handler: Any): Unit = {
    handler match {
      case som: ScriptObjectMirror =>
        val arg = Match(SimpleTerminalTreeNode("root", "x"), Collections.emptyList())
        val args = Seq(arg)
        som.call("apply", args:_*)
    }
  }

  override val registry = Map(
    "PathExpressionEngine" -> new PathExpressionExposer
  )
}
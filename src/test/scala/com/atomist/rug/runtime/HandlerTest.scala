package com.atomist.rug.runtime

import java.util.Collections

import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.kind.service.{ConsoleMessageBuilder, EmptyActionRegistry}
import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.rug.runtime.js.interop.{AtomistFacade, Match, PathExpressionExposer}
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.SimpleTerminalTreeNode
import jdk.nashorn.api.scripting.ScriptObjectMirror
import org.scalatest.{FlatSpec, Matchers}

class HandlerTest extends FlatSpec with Matchers {

  it should "allow Atomist invocations" in {

    val jsc = new JavaScriptContext

    val tsc = new TypeScriptCompiler

    val subscription =
      s"""
        |import {Atomist} from "user-model/operations/Handler"
        |import {Project,File} from "user-model/model/Core"
        |
        |declare var atomist: Atomist  // <= this is for the compiler only
        |
        |declare var print: any
        |
        |atomist.messageBuilder().say("This is a test").on("channel").send()
        |
        |atomist.on<Project,File>('project/src/main/**.java', m => {
        |   //print(`in handler with $${m}`)
        |   //print(`Root=$${m.root()}, leaves=$${m.matches()}`)
        |})
      """.stripMargin
    val r = tsc.compile(SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/handlers/sub1.ts", subscription)
    ))

    jsc.engine.put("atomist", TestAtomistFacade)

    for (ts <- r.allFiles.filter(_.name.endsWith(".js"))) {
      jsc.eval(ts)
    }

    jsc.shutdown()
  }
}


object TestAtomistFacade extends AtomistFacade {

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

  override def messageBuilder = new ConsoleMessageBuilder("TEAM_ID", EmptyActionRegistry)
}

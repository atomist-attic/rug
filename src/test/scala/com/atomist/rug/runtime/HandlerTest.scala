package com.atomist.rug.runtime

import java.util.Collections
import javax.script.SimpleBindings

import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.rug.kind.service.{ConsoleMessageBuilder, EmptyActionRegistry}
import com.atomist.rug.runtime.js.{JavaScriptContext, JavaScriptHandlerFinder}
import com.atomist.rug.runtime.js.interop._
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.SimpleTerminalTreeNode
import jdk.nashorn.api.scripting.ScriptObjectMirror
import org.scalatest.{FlatSpec, Matchers}

class HandlerTest extends FlatSpec with Matchers {

  it should "allow Atomist invocations" in {

    val subscription =
      s"""
         |import {Atomist} from "@atomist/rug/operations/Handler"
         |import {Project,File} from "@atomist/rug/model/Core"
         |
        |declare var atomist: Atomist  // <= this is for the compiler only
         |
        |declare var print: any
         |
        |atomist.messageBuilder().say("This is a test").on("channel").send()
         |
        |atomist.on<Project,File>('/src/main//*.java', m => {
         |   //print(`in handler with $${m}`)
         |   //print(`Root=$${m.root()}, leaves=$${m.matches()}`)
         |})
      """.stripMargin
    val r = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/handlers/sub1.ts", subscription)
    ))

    val bindings = new SimpleBindings()
    bindings.put("atomist", TestAtomistFacade)
    val jsc = new JavaScriptContext(r, DefaultAtomistConfig, bindings)


    for (ts <- r.allFiles.filter(_.name.endsWith(".js"))) {
      //TODO - call compiler
      //jsc.eval(ts)
    }

  }

  it should "find and invoke other style of handler" in {
    val r = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(NamedJavaScriptEventHandlerTest.reOpenCloseIssueProgram, NamedJavaScriptEventHandlerTest.issuesStuff))
    val ctx = new JavaScriptHandlerContext(null,null,null)
    JavaScriptHandlerFinder.fromJavaScriptArchive(r,ctx)
  }
}

object TestAtomistFacade extends AtomistFacade {

  override def teamId: String = "foobar"

  def on(s: String, handler: Any): Unit = {
    handler match {
      case som: ScriptObjectMirror =>
        val arg = jsMatch(SimpleTerminalTreeNode("root", "x"), Collections.emptyList())
        val args = Seq(arg)
        som.call("apply", args: _*)
    }
  }

  override val registry = Map(
    "PathExpressionEngine" -> new jsPathExpressionEngine(this)
  )

  override def messageBuilder = new ConsoleMessageBuilder("TEAM_ID", EmptyActionRegistry)
}

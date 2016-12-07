package com.atomist.rug.runtime

import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.rug.runtime.js.interop.DefaultAtomistFacade
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
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
        |atomist.on<Project,File>('TYPESCRIPT/src/main/**.java', m => {
        |   print(`in handler with $${m}`)
        |   print(`Root=$${m.root()}, leaves=$${m.matches()}`)
        |})
      """.stripMargin
    val r = tsc.compile(SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/handlers/sub1.ts", subscription)
    ))

    jsc.engine.put("atomist", DefaultAtomistFacade)

    for (ts <- r.allFiles.filter(_.name.endsWith(".js"))) {
      jsc.eval(ts)
    }

    jsc.shutdown()
  }
}


package com.atomist.rug.runtime

import com.atomist.rug.runtime.js.JavaScriptEventHandler
import com.atomist.rug.runtime.js.interop._
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.SimpleFileBasedArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class HandlerTest extends FlatSpec with Matchers {
  it should "find and invoke other style of handler" in {
    val r = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(JavaScriptEventHandlerTest.reOpenCloseIssueProgram))
    val ctx = new JavaScriptHandlerContext(null,null)
    JavaScriptEventHandler.extractHandlers(r,ctx)
  }
}
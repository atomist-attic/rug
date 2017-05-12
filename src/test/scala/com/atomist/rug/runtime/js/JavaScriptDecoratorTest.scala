package com.atomist.rug.runtime.js

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.RugArchiveReader
import com.atomist.rug.TestUtils.contentOf
import com.atomist.rug.runtime.js.JavaScriptEventHandlerTest.atomistConfig
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FunSpec, Matchers}

class JavaScriptDecoratorTest extends FunSpec with Matchers {

  val emit = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "EmitHandler.ts"))

  it("should be able to emit a Rug by calling underlying decorator logic") {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(emit))
    val rugs = RugArchiveReader(rugArchive)
    assert(rugs.commandHandlers.size == 2)
    assert(rugs.commandHandlers.head.name == "BoringCommand")
    assert(rugs.commandHandlers.head.description == "description only")
    assert(rugs.commandHandlers.last.description == "description")
    assert(rugs.commandHandlers.last.name == "andName")
  }
  val noUnderscoreStuff = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "SimpleHandlerWithHiddenProperties.ts"))

  it("should not expose decorated object properties in 'this'") {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(noUnderscoreStuff))
    val rugs = RugArchiveReader(rugArchive)
    rugs.commandHandlers.head.handle(null, SimpleParameterValues.Empty)
  }
}

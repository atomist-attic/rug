package com.atomist.rug.runtime.js

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.RugArchiveReader
import com.atomist.rug.TestUtils.contentOf
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FunSpec, Matchers}

class JavaScriptUtilsTest  extends FunSpec with Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  val parameterInherritance = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "HandlerWithInherritedParameters.ts"))

  it("should inherit parameters from the prototype") {
    val rugArchive = TypeScriptBuilder.compileWithExtendedModel(
      SimpleFileBasedArtifactSource(parameterInherritance))
    val rugs = RugArchiveReader(rugArchive)
    val handler = rugs.commandHandlers.head
    val params = handler.parameters
    assert(params.size === 3)
    assert(params(1).name === "foo")
    assert(params(1).description === "child")
    assert(params(2).name === "bar")
    assert(params.head.name === "baz")
    assert(params.head.description === "dup")
  }

  it("should inherit mapped parameters from the prototype") {
    val rugArchive = TypeScriptBuilder.compileWithExtendedModel(
      SimpleFileBasedArtifactSource(parameterInherritance))
    val rugs = RugArchiveReader(rugArchive)
    val handler = rugs.commandHandlers.head
    val params = handler.mappedParameters
    assert(params.size === 3)
    assert(params.head.localKey === "really")
    assert(params.head.foreignKey === "manual")
    assert(params(1).localKey === "blah")
    assert(params(1).foreignKey === "blah-child")
    assert(params(2).localKey === "quz")
    assert(params(2).foreignKey === "quz-parent")
  }
}

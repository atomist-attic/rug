package com.atomist.project.archive

import com.atomist.plan.TreeMaterializer
import com.atomist.rug.runtime.js.TestTreeMaterializer
import com.atomist.rug.runtime.js.interop.JavaScriptHandlerContext
import com.atomist.rug.runtime.lang.js.NashornConstructorTest
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.source.file.ClassPathArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class JavaScriptRugArchiveReaderTest extends FlatSpec with Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig
  val treeMaterializer: TreeMaterializer = TestTreeMaterializer

  it should "load handlers of different kinds from an archive" in {
    val ts = ClassPathArtifactSource.toArtifactSource("com/atomist/project/archive/MyHandlers.ts")
    val moved = ts.withPathAbove(".atomist/handlers")
    val as = TypeScriptBuilder.compileWithModel(moved)
    val reader = new JavaScriptRugArchiveReader(new JavaScriptHandlerContext("XX", treeMaterializer))
    val ops = reader.find(as, None, Nil)
    assert(ops.responseHandlers.size === 2)
    assert(ops.commandHandlers.size === 2)
    assert(ops.eventHandlers.size === 1)
  }
  it should "find and invoke plain javascript generators" in {
    val apc =  new JavaScriptRugArchiveReader(new JavaScriptHandlerContext("XX", treeMaterializer))
    val f1 = StringFileArtifact("package.json", "{}")
    val f2 = StringFileArtifact("app/Thing.js", "var Thing = {};")

    val rugAs = SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/SimpleEditor.js",
        NashornConstructorTest.SimpleJavascriptEditor),
      f1,
      f2
    ) + TypeScriptBuilder.userModel

    val ops = apc.find(rugAs, None, Nil)
    assert(ops.editors.size === 1)
    assert(ops.editors.head.parameters.size === 1)
  }
}

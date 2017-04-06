package com.atomist.tree.content.text

import com.atomist.param.SimpleParameterValues
import com.atomist.parse.java.ParsingTargets
import com.atomist.rug.RugArchiveReader
import com.atomist.rug.runtime.js.JavaScriptProjectEditor
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.file.ClassPathArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class OverwriteableTextTreeNodeTypeScriptTest extends FlatSpec with Matchers {

  it should "use OverwriteableTextTreeNode from TypeScript" in {
    // RUN THIS EDITOR
    val tsEditorResource = "com/atomist/tree/content/text/OverwriteableTextTreeNodeTypeScriptTest.ts"
    val parameters = SimpleParameterValues.Empty

    // ON THIS PROJECT
    val target = ParsingTargets.NewStartSpringIoProject

    // construct the Rug archive
    val artifactSourceWithEditor = ClassPathArtifactSource.toArtifactSource(tsEditorResource).withPathAbove(".atomist/editors")
    val artifactSourceWithRugNpmModule = TypeScriptBuilder.compileWithModel(artifactSourceWithEditor)

    // get the operation out of the artifact source
    val projectEditor = RugArchiveReader(artifactSourceWithRugNpmModule).editors.head.asInstanceOf[JavaScriptProjectEditor]

    // apply the operation
    try {
      projectEditor.modify(target, parameters)
      fail("OutOfDateNodeException should have been thrown")
    } catch {
      case r: RuntimeException =>
        assert(r.getCause.isInstanceOf[OutOfDateNodeException])
    }
  }
}

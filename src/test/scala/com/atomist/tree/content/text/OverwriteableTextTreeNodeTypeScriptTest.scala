package com.atomist.tree.content.text

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.runtime.js.{JavaScriptInvokingProjectEditor, JavaScriptOperationFinder}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.file.ClassPathArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class OverwriteableTextTreeNodeTypeScriptTest extends FlatSpec with Matchers {

  it should "use OverwriteableTextTreeNode from TypeScript" in {

    // RUN THIS EDITOR
    val tsEditorResource = "com/atomist/tree/content/text/OverwriteableTextTreeNodeTypeScriptTest.ts"
    val parameters = SimpleProjectOperationArguments.Empty

    // ON THIS PROJECT
    val target = ParsingTargets.NewStartSpringIoProject
    val fileThatWillBeModified = "pom.xml"

    // construct the Rug archive
    val artifactSourceWithEditor = ClassPathArtifactSource.toArtifactSource(tsEditorResource).withPathAbove(".atomist/editors")
    val artifactSourceWithRugNpmModule = TypeScriptBuilder.compileWithModel(artifactSourceWithEditor)
    //println(s"rug archive: $artifactSourceWithEditor")

    // get the operation out of the artifact source
    val projectEditor = JavaScriptOperationFinder.fromJavaScriptArchive(artifactSourceWithRugNpmModule).head.asInstanceOf[JavaScriptInvokingProjectEditor]

    // apply the operation

    an[OutOfDateNodeException] should be thrownBy {
      projectEditor.modify(target, parameters)
    }
  }
}

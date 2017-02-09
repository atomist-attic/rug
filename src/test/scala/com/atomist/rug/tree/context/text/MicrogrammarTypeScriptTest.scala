package com.atomist.rug.tree.context.text

import java.io.File

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.runtime.js.{JavaScriptInvokingProjectEditor, JavaScriptOperationFinder}
import com.atomist.source.file.{ClassPathArtifactSource, FileSystemArtifactSource, FileSystemArtifactSourceIdentifier}
import org.scalatest.{FlatSpec, Matchers}
import com.atomist.rug.ts.TypeScriptBuilder

class MicrogrammarTypeScriptTest extends FlatSpec with Matchers {

  it should "use Microgrammar from TypeScript" in {

    val tsEditorResource = "com/atomist/rug/tree/context/text/MicrogrammarTypeScriptTest.ts"
    val parameters = SimpleProjectOperationArguments.Empty
    val target = FileSystemArtifactSource(FileSystemArtifactSourceIdentifier(new File("src/test/scala/com/atomist/rug/tree/context/text/MicrogrammarTypeScriptTest.scala")))
    val fileThatWillBeModified = "MicrogrammarTypeScriptTest.scala"


    // construct the Rug archive
    val artifactSourceWithEditor = ClassPathArtifactSource.toArtifactSource(tsEditorResource).withPathAbove(".atomist/editors")
    val artifactSourceWithRugNpmModule = TypeScriptBuilder.compileWithModel(artifactSourceWithEditor)

    // get the operation out of the artifact source
    val projectEditor = JavaScriptOperationFinder.fromJavaScriptArchive(artifactSourceWithRugNpmModule).head.asInstanceOf[JavaScriptInvokingProjectEditor]

    // apply the operation
    projectEditor.modify(target, parameters) match {
      case sm: SuccessfulModification =>
        val contents = sm.result.findFile(fileThatWillBeModified).get.content
        withClue(s"contents of $fileThatWillBeModified are:<$contents>") {
          // check the results
          contents.contains("object TheTestHasChangedThis {") should be(true)
        }

      case boo => fail(s"Modification was not successful: $boo")
    }

  }

}

object TheTestWillChangeThis {

  // at least it's right here where you can look at it

}

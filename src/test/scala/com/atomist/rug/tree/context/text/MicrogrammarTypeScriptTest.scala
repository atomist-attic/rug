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

  it should "Apply a sample editor " in {
    val tsEditorResource = "com/atomist/rug/tree/context/text/MicrogrammarTypeScriptSampleEditor.ts"
    val parameters = SimpleProjectOperationArguments.Empty
    val target = FileSystemArtifactSource(FileSystemArtifactSourceIdentifier(
      new File("src/test/scala/com/atomist/rug/tree/context/text/MicrogrammarTypeScriptTest.scala")))
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
          //println("\n" + contents + "\n")
          contents.contains("val matches = strictMatchAndFakeAFile(aWasaB, \"Henry was aged 19\")") should be(true)
        }
      case boo =>
        println("The source file contained <" + target.files.head.content + ">")
        fail(s"Modification was not successful: $boo")
    }
  }

}

object TheTestWillChangeThis {

  val sampleScalaForUsefulEditorTest =
    """  it should "parse 1 match of 2 parts in whole string" in {
    |    val Right(matches) = aWasaB.strictMatch("Henry was aged 19")
    |    assert(matches.count === 2)
    |    matches.childrenNamed("name").head match {
    |      case sm: MutableTerminalTreeNode =>
    |        assert(sm.value === "Henry")
    |    }
    |    matches.childrenNamed("age").head match {
    |      case sm: MutableTerminalTreeNode =>
    |        assert(sm.value === "19")
    |    }
    |  }"""

}

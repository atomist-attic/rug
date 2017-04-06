package com.atomist.tree.content.text.microgrammar

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.RugArchiveReader
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.source.file.ClassPathArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class MultipleUseOfSubmatcherTypeScriptTest extends FlatSpec with Matchers {

  val inputFile = "I like broccoli, carrots, bananas, and ponies."
  val modifiedFile = "I like (1) broccoli, (2) carrots, (3) bananas, and (4) ponies."

  def singleFileArtifactSource(name: String, content:String):ArtifactSource =
    new SimpleFileBasedArtifactSource("whatever", Seq(StringFileArtifact(name, content)))

  it should "use multiple submatchers" in {

    val tsEditorResource = "com/atomist/tree/content/text/microgrammar/MultipleUseOfSubmatcherTypeScriptTest.ts"
    val parameters = SimpleParameterValues.Empty
    val fileThatWillBeModified = "targetFile"
    val target = singleFileArtifactSource(fileThatWillBeModified, inputFile)

    // construct the Rug archive
    val artifactSourceWithEditor = ClassPathArtifactSource.toArtifactSource(tsEditorResource).withPathAbove(".atomist/editors")
    val artifactSourceWithRugNpmModule = TypeScriptBuilder.compileWithModel(artifactSourceWithEditor)
    //println(s"rug archive: $artifactSourceWithEditor")

    // get the operation out of the artifact source
    val projectEditor = RugArchiveReader(artifactSourceWithRugNpmModule).editors.head

    // apply the operation
    projectEditor.modify(target, parameters) match {
      case sm: SuccessfulModification =>
        val contents = sm.result.findFile(fileThatWillBeModified).get.content
        withClue(s"contents of $fileThatWillBeModified are:<$contents>") {
          // check the results
          contents should be(modifiedFile)
        }

      case boo => fail(s"Modification was not successful: $boo")
    }

  }

}

package com.atomist.tree.content.text.microgrammar

import com.atomist.param.SimpleParameterValues
import com.atomist.project.archive.RugArchiveReader
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.source.file.ClassPathArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class MatcherMicrogrammarConstructionTypeScriptTest extends FlatSpec with Matchers {

  val inputFile = "I like broccoli. I like carrots. I like bananas."
  val modifiedFile = "I like broccoli (which is a vegetable). I like carrots (which is a vegetable). I like bananas."

  def singleFileArtifactSource(name: String, content:String):ArtifactSource =
    new SimpleFileBasedArtifactSource("whatever", Seq(StringFileArtifact(name, content)))

  it should "use MatcherMicrogrammarConstruction from TypeScript" in {

    val tsEditorResource = "com/atomist/tree/content/text/microgrammar/MatcherMicrogrammarConstructionTypeScriptTest.ts"
    val parameters = SimpleParameterValues.Empty
    val fileThatWillBeModified = "targetFile"
    val target = singleFileArtifactSource(fileThatWillBeModified, inputFile)

    // construct the Rug archive
    val artifactSourceWithEditor = ClassPathArtifactSource.toArtifactSource(tsEditorResource).withPathAbove(".atomist/editors")
    val artifactSourceWithRugNpmModule = TypeScriptBuilder.compileWithModel(artifactSourceWithEditor)
    //println(s"rug archive: $artifactSourceWithEditor")

    // get the operation out of the artifact source
    val projectEditor = RugArchiveReader.find(artifactSourceWithRugNpmModule).editors.head

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

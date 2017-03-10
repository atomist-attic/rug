package com.atomist.tree.content.text.microgrammar

import com.atomist.param.SimpleParameterValues
import com.atomist.project.archive.RugArchiveReader
import com.atomist.parse.java.ParsingTargets
import com.atomist.project.edit.SuccessfulModification
import com.atomist.source.file.ClassPathArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class MatcherMicrogrammarConstructionTypeScriptTest extends FlatSpec with Matchers {

  it should "use MatcherMicrogrammarConstruction from TypeScript" in {

    val tsEditorResource = "com/atomist/tree/content/text/microgrammar/MatcherMicrogrammarConstructionTypeScriptTest.ts"
    val parameters = SimpleParameterValues.Empty
    val target = ParsingTargets.NewStartSpringIoProject
    val fileThatWillBeModified = "pom.xml"

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
          contents.contains("dependenciesAreForBirds") should be(true)
        }

      case boo => fail(s"Modification was not successful: $boo")
    }

  }

}

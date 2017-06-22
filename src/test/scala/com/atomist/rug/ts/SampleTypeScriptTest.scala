package com.atomist.rug.ts

import com.atomist.param.SimpleParameterValues
import com.atomist.parse.java.ParsingTargets
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.RugArchiveReader
import com.atomist.source.file.ClassPathArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class SampleTypeScriptTest extends FlatSpec with Matchers {

  it should "use Sample from TypeScript" in {

    val tsEditorResource = "com/atomist/rug/ts/SampleTypeScriptTest.ts"
    val parameters = SimpleParameterValues.Empty
    val target = ParsingTargets.NewStartSpringIoProject
    val fileThatWillBeModified = "pom.xml"

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
          contents.contains("dependenciesAreForBirds") shouldBe true
        }

      case boo => fail(s"Modification was not successful: $boo")
    }

  }

}

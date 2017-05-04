package com.atomist.tree.content.text.microgrammar

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.RugArchiveReader
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.source.file.ClassPathArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class MatcherMicrogrammarConstructionTypeScriptTest extends FlatSpec with Matchers {

  /*
   The matchers we can construct are:
   - regex
   - or (alternate)
   - optional (0 or 1)
   - repeat (0 or more)

   These can be combined with concatenation in a string that references submatchers.
   */

  /*
   The microgrammar is in the TypeScript file. Let's make it look like this.

   `public $returnType $typeParameters $functionName($params)`, {
       returnType: "$javaType",
       typeParameters: Optional("<$typeVariable>"),
       typeVariable: Regex("[A-Z]\w*"),
       functionName: "$javaIdentifier",
       params: Repeat("$param"),
       param: "$javaType $javaIdentifier$comma",
       comma: Optional(","),
       javaType: Regex("[A-Za-z0-9_]_+"),
       javaIdentifier: Regex("[a-z]\w*")
   }
   */

  val inputFile =
    """public Banana pick(String color, int spots): Precise {
      |   // and then some stuff
      |}
      |""".stripMargin
  val modifiedFile =
    """public Fruit grow(int qty): Precise {
      |   // and then some stuff
      |}
      |""".stripMargin

  // testing explicit concat and literal: two spaces after colon should not match
  val unmatchingInput =
    """public Banana pick(String color, int spots):  Precise {
      |   // and then some stuff
      |}
      |""".stripMargin

  def singleFileArtifactSource(name: String, content: String): ArtifactSource =
    new SimpleFileBasedArtifactSource("whatever", Seq(StringFileArtifact(name, content)))

  it should "use MatcherMicrogrammarConstruction from TypeScript" in {

    val tsEditorResource = "com/atomist/tree/content/text/microgrammar/MatcherMicrogrammarConstructionTypeScriptTest.ts"
    val parameters = SimpleParameterValues.Empty
    val fileThatWillBeModified = "targetFile"
    val target = singleFileArtifactSource(fileThatWillBeModified, inputFile) + singleFileArtifactSource("unmatchingFile", unmatchingInput)

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

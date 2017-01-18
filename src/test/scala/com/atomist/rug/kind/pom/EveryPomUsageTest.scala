package com.atomist.rug.kind.pom

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class EveryPomUsageTest extends FlatSpec with Matchers {

  import com.atomist.rug.TestUtils._

  private def runProgAndCheck(as: ArtifactSource, mods: Int): ArtifactSource = {
    val prog =
      """
        |editor EveryPomEdit
        |with Project p
        |  with EveryPom o
        |    do setGroupId "mygroup"
      """.stripMargin

    val progArtifact: ArtifactSource = new SimpleFileBasedArtifactSource(DefaultRugArchive,
      StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog)
    )

    val result = doModification(progArtifact, as, EmptyArtifactSource(""),
      SimpleProjectOperationArguments("", Map.empty[String,Object]))

    result.cachedDeltas.size should be(mods)

    result
  }

  private val pomFileArtifact = JavaTypeUsageTest.NewSpringBootProject.findFile("pom.xml").get

  it should "edit a single pom" in {
    val singlePom: ArtifactSource = new SimpleFileBasedArtifactSource("simple", pomFileArtifact)
    runProgAndCheck(singlePom, 1)
  }

  it should "edit three poms" in {
    val triplePom: ArtifactSource = new SimpleFileBasedArtifactSource("simple",
      Seq(pomFileArtifact,
        StringFileArtifact("module1/pom.xml", pomFileArtifact.content),
        StringFileArtifact("module2/but/further/nested/pom.xml", pomFileArtifact.content)
      )
    )
    runProgAndCheck(triplePom, 3)
  }

  it should "edit three poms and ignore other files" in {
    val triplePomPlus: ArtifactSource = new SimpleFileBasedArtifactSource("simple",
      Seq(pomFileArtifact,
        StringFileArtifact("README.md", "This is a README\n"),
        StringFileArtifact("module1/pom.xml", pomFileArtifact.content),
        StringFileArtifact("src/main/Some.java", "class Some {}\n"),
        StringFileArtifact("module2/but/further/nested/pom.xml", pomFileArtifact.content)
      )
    )
    runProgAndCheck(triplePomPlus, 3)
  }
}

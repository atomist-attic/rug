package com.atomist.rug.kind.pom

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{ModificationAttempt, NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class PomUsageTest extends FlatSpec with Matchers with LazyLogging {

  import com.atomist.rug.TestUtils._

  it should "update group id with no change with native Rug function" in {
    val prog =
      """
        |editor PomEdit
        |
        |with Pom x when path = "pom.xml"
        |  do groupId
      """.stripMargin

    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case nmn: NoModificationNeeded =>
      case wtf => fail(s"Expected NoModificationNeeded, not $wtf")
    }
  }

  it should "update an existing property" in {
    val prog =
      """
        |editor PomEdit
        |
        |with Pom p when path = "pom.xml"
        |  do setGroupId "mygroup"
      """.stripMargin

    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case success: SuccessfulModification =>
      case _ => ???
    }
  }

  it should "add a new dependency" in {
    val prog =
      """
        |editor PomEdit
        |
        |with Pom p when path = "pom.xml"
        |  do addOrReplaceDependency "mygroup" "myartifact"
      """.stripMargin

    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case success: SuccessfulModification =>
      case _ => ???
    }
  }

  // Return new content
  private def updateWith(prog: String, project: ArtifactSource): ModificationAttempt = {
    val newName = "Foo"
    val pas = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog))

    attemptModification(pas, project, EmptyArtifactSource(""), SimpleParameterValues( Map(
      "new_name" -> newName
    )))
  }
}

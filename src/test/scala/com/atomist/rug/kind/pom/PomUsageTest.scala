package com.atomist.rug.kind.pom

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{ModificationAttempt, NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.atomist.source.{ArtifactSource, EmptyArtifactSource}
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
        |do groupId
      """.stripMargin

    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case nmn: NoModificationNeeded =>
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
      case success: SuccessfulModification => logger.debug("" + success.impacts)
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
      case success: SuccessfulModification => logger.debug("" + success.impacts)
    }
  }

  // Return new content
  private def updateWith(prog: String, project: ArtifactSource): ModificationAttempt = {
    val filename = "thing.yml"

    val newName = "Foo"
    attemptModification(prog, project, EmptyArtifactSource(""), SimpleProjectOperationArguments("", Map(
      "new_name" -> newName
    )))
  }
}

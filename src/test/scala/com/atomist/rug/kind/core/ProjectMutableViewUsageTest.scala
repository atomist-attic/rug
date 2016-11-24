package com.atomist.rug.kind.core

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.ModificationAttempt
import com.atomist.source.{ArtifactSource, EmptyArtifactSource}
import org.scalatest.{FlatSpec, Matchers}

class ProjectMutableViewUsageTest extends FlatSpec with Matchers {

  import com.atomist.rug.TestUtils._

  it should "copy a file" is pending
//  {
//    val prog =
//      """
//        |editor Copy1
//        |
//        |with pom x when path = "pom.xml"
//        |do groupId
//      """.stripMargin
//
//    updateWith(prog, JavaClassTypeUsageTest.NewSpringBootProject) match {
//      case nmn: NoModificationNeeded =>
//    }
//  }

  // Return new content
  private def updateWith(prog: String, project: ArtifactSource): ModificationAttempt = {
    val filename = "thing.yml"

    val newName = "Foo"
    attemptModification(prog, project, EmptyArtifactSource(""), SimpleProjectOperationArguments("", Map(
      "new_name" -> newName
    )))
  }
}

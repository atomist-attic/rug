package com.atomist.rug.kind.core

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.ModificationAttempt
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
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
    val pas = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog))

    attemptModification(pas, project, EmptyArtifactSource(""), SimpleParameterValues(Map(
      "new_name" -> newName
    )))
  }
}

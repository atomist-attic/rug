package com.atomist.rug.kind.properties

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{ModificationAttempt, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class PropertiesUsageTest extends FlatSpec with Matchers with LazyLogging {

  import com.atomist.rug.TestUtils._

  it should "update an existing property" in {
    val prog =
      """
        |editor PropertiesEdit
        |
        |with Properties p when path = "src/main/resources/application.properties"
        |do setProperty "server.port" "8181"
      """.stripMargin

    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case success: SuccessfulModification =>
      case _ => ???
    }
  }

  it should "create a new property" in {
    val prog =
      """
        |editor PropertiesEdit
        |
        |with Properties p when path = "src/main/resources/application.properties"
        |do setProperty "server.portlet" "8181"
      """.stripMargin

    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case success: SuccessfulModification =>
      case _ => ???
    }
  }

  // Return new content
  private def updateWith(prog: String, project: ArtifactSource): ModificationAttempt = {
    val filename = "thing.yml"

    val newName = "Foo"
    val pas = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog))

    attemptModification(pas, project, EmptyArtifactSource(""), SimpleParameterValues( Map(
      "new_name" -> newName
    )))
  }
}

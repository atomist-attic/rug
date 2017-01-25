package com.atomist.rug.kind.xml

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{ModificationAttempt, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class XmlFileTypeUsageTest extends FlatSpec with Matchers {

  import com.atomist.rug.TestUtils._

  it should "update group id with native Rug function" in {
    val prog =
      """
        |editor Xit
        |
        |let pe = $(/*[@name='pom.xml']/XmlFile()/project/groupId)
        |
        |with pe
        |  do update "<groupId>not-atomist</groupId>"
        |""".stripMargin

    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case sm: SuccessfulModification =>
        val outputxml = sm.result.findFile("pom.xml").get
        outputxml.content.contains("<groupId>not-atomist</groupId>") should be(true)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
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

package com.atomist.rug.kind.xml

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{ModificationAttempt, NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class XmlTypeUsageTest extends FlatSpec with Matchers {

  import com.atomist.rug.TestUtils._

  it should "update group id with no change with native Rug function" in {
    val prog =
      """
        |editor Xit
        |
        |with Xml x when path = "pom.xml"
        |do getTextContentFor "/project/groupId"
      """.stripMargin

    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case nmn: NoModificationNeeded =>
      case wtf =>  fail(s"Expected NoModificationNeeded, not $wtf")
    }
  }

  it should "update group id with native Rug function" in {
    val prog =
      """
        |editor Xit
        |
        |with Xml x when path = "pom.xml"
        |do addOrReplaceNode "/project" "/project/groupId" "groupId" "<groupId>not-atomist</groupId>"
      """.stripMargin

    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case sm: SuccessfulModification =>
        val outputxml = sm.result.findFile("pom.xml").get
        outputxml.content.contains("<groupId>not-atomist</groupId>") should be(true)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "add a new child block with native Rug function" in {
    val prog =
      """
        |editor Xit
        |
        |with Xml x when path = "pom.xml"
        |do addOrReplaceNode "/project/build/plugins" "/project/build/plugins/plugin" "plugin" "<plugin><groupId>com.atomist</groupId><artifactId>our-great-plugin</artifactId></plugin>"
      """.stripMargin

    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case sm: SuccessfulModification =>
        val outputxml = sm.result.findFile("pom.xml").get
        outputxml.content.contains("<artifactId>our-great-plugin</artifactId>") should be(true)
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

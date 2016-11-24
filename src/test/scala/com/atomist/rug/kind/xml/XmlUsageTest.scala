package com.atomist.rug.kind.xml

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{ModificationAttempt, NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.kind.java.JavaClassTypeUsageTest
import com.atomist.source.{ArtifactSource, EmptyArtifactSource}
import org.scalatest.{FlatSpec, Matchers}

class XmlUsageTest extends FlatSpec with Matchers {

  import com.atomist.rug.TestUtils._

  it should "update group id with no change with native Rug function" in {
    val prog =
      """
        |editor Xit
        |
        |with xml x when path = "pom.xml"
        |do getTextContentFor "/project/groupId"
      """.stripMargin

    updateWith(prog, JavaClassTypeUsageTest.NewSpringBootProject) match {
      case nmn: NoModificationNeeded =>
    }
  }

  it should "update group id with native Rug function" in {
    val prog =
      """
        |editor Xit
        |
        |with xml x when path = "pom.xml"
        |do addOrReplaceNode "/project" "/project/groupId" "groupId" "<groupId>not-atomist</groupId>"
      """.stripMargin

    updateWith(prog, JavaClassTypeUsageTest.NewSpringBootProject) match {
      case sm: SuccessfulModification =>
        val outputxml = sm.result.findFile("pom.xml").get
        outputxml.content.contains("<groupId>not-atomist</groupId>") should be(true)
    }
  }

  it should "add a new child block with native Rug function" in {
    val prog =
      """
        |editor Xit
        |
        |with xml x when path = "pom.xml"
        |do addOrReplaceNode "/project/build/plugins" "/project/build/plugins/plugin" "plugin" "<plugin><groupId>com.atomist</groupId><artifactId>our-great-plugin</artifactId></plugin>"
      """.stripMargin

    updateWith(prog, JavaClassTypeUsageTest.NewSpringBootProject) match {
      case sm: SuccessfulModification =>
        val outputxml = sm.result.findFile("pom.xml").get
        outputxml.content.contains("<artifactId>our-great-plugin</artifactId>") should be(true)
    }
  }

  // Return new content
  private def updateWith(prog: String, project: ArtifactSource): ModificationAttempt = {

    val newName = "Foo"
    attemptModification(prog, project, EmptyArtifactSource(""), SimpleProjectOperationArguments("", Map(
      "new_name" -> newName
    )))
  }
}

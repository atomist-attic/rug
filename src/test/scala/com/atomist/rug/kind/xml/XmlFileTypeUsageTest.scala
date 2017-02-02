package com.atomist.rug.kind.xml

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{ModificationAttempt, NoModificationNeeded, SuccessfulModification}
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
        //println(outputxml.content)
        outputxml.content.contains("<groupId>not-atomist</groupId>") should be(true)
      case _ => ???
    }
  }

//  it should "add a new child block with native Rug function" in {
//    val prog =
//      """
//        |editor Xit
//        |
//        |with Xml x when path = "pom.xml"
//        |do addOrReplaceNode "/project/build/plugins" "/project/build/plugins/plugin" "plugin" "<plugin><groupId>com.atomist</groupId><artifactId>our-great-plugin</artifactId></plugin>"
//      """.stripMargin
//
//    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
//      case sm: SuccessfulModification =>
//        val outputxml = sm.result.findFile("pom.xml").get
//        outputxml.content.contains("<artifactId>our-great-plugin</artifactId>") should be(true)
//    }
//  }

  // Return new content
  private def updateWith(prog: String, project: ArtifactSource): ModificationAttempt = {

    val newName = "Foo"
    val pas = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog))

    attemptModification(pas, project, EmptyArtifactSource(""), SimpleProjectOperationArguments("", Map(
      "new_name" -> newName
    )))
  }
}

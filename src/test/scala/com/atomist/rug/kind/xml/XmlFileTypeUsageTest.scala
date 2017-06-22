package com.atomist.rug.kind.xml

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.TestUtils
import com.atomist.rug.kind.java.JavaTypeUsageTest
import org.scalatest.{FlatSpec, Matchers}

class XmlFileTypeUsageTest extends FlatSpec with Matchers {

  "XmlFileType" should "update group id with native Rug function" in {
    val ed = TestUtils.editorInSideFile(this, "Xit.ts")
    ed.modify(JavaTypeUsageTest.NewSpringBootProject, SimpleParameterValues.Empty) match {
      case sm: SuccessfulModification =>
        val outputxml = sm.result.findFile("pom.xml").get
        outputxml.content.contains("<groupId>not-atomist</groupId>") shouldBe true
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "update version when needs upgrade" in {
    val ed = TestUtils.editorInSideFile(this, "UpgradeVersion.ts")
    val desiredVersion = "2.4.3"
    ed.modify(JavaTypeUsageTest.NewSpringBootProject,
      SimpleParameterValues(Map(
        "group" -> "testgroup",
        "artifact" -> "testartifact",
        "desiredVersion" -> desiredVersion
      ))
    )
    match {
      case sm: SuccessfulModification =>
        val outputxml = sm.result.findFile("pom.xml").get
        //println(outputxml.content)
        outputxml.content.contains(s"<version>$desiredVersion</version>") shouldBe true
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

}

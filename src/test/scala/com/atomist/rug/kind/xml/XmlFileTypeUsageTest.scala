package com.atomist.rug.kind.xml

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.TestUtils
import com.atomist.rug.kind.java.JavaTypeUsageTest
import org.scalatest.{FlatSpec, Matchers}

class XmlFileTypeUsageTest extends FlatSpec with Matchers {

  it should "update group id with native Rug function" in {
    val ed = TestUtils.editorInSideFile(this, "Xit.ts")
    ed.modify(JavaTypeUsageTest.NewSpringBootProject, SimpleParameterValues.Empty) match {
      case sm: SuccessfulModification =>
        val outputxml = sm.result.findFile("pom.xml").get
        outputxml.content.contains("<groupId>not-atomist</groupId>") should be(true)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

}

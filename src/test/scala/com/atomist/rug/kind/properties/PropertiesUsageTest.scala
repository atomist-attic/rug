package com.atomist.rug.kind.properties

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.TestUtils
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class PropertiesUsageTest extends FlatSpec with Matchers with LazyLogging {

  lazy val ed = TestUtils.editorInSideFile(this, "SetProperty.ts")

  it should "update an existing property" in {
    ed.modify(JavaTypeUsageTest.NewSpringBootProject,
      SimpleParameterValues("value", "server.port")) match {
      case success: SuccessfulModification =>
      case _ => ???
    }
  }

  it should "create a new property" in {
    ed.modify(JavaTypeUsageTest.NewSpringBootProject,
      SimpleParameterValues("value", "server.portlet")) match {
      case success: SuccessfulModification =>
      case _ => ???
    }
  }

}

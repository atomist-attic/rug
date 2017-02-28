package com.atomist.rug.kind.pom

import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class PomUsageTest extends FlatSpec with Matchers with LazyLogging {

  import com.atomist.rug.TestUtils._

  it should "update an existing property" in {
    val ed = editorInSideFile(this, "UpdateProperty.ts")
    ed.modify(JavaTypeUsageTest.NewSpringBootProject) match {
      case _: SuccessfulModification =>
      case _ => ???
    }
  }

  it should "add a new dependency" in {
    val ed = editorInSideFile(this, "AddOrReplaceDependency.ts")
    ed.modify(JavaTypeUsageTest.NewSpringBootProject) match {
      case success: SuccessfulModification =>
      case _ => ???
    }
  }

}

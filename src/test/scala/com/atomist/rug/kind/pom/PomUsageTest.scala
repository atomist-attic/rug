package com.atomist.rug.kind.pom

import com.atomist.project.edit.{NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class PomUsageTest extends FlatSpec with Matchers with LazyLogging {

  import com.atomist.rug.TestUtils._

  it should "update group id with no change with native Rug function" in {
    val prog =
      """
        |editor PomEdit
        |
        |with Pom x when path = "pom.xml"
        |  do groupId
      """.stripMargin

    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case nmn: NoModificationNeeded =>
      case wtf => fail(s"Expected NoModificationNeeded, not $wtf")
    }
  }

  it should "update an existing property" in {
    val prog =
      s"""
        |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
        |import {Project,Pom} from '@atomist/rug/model/Core'
        |import {Editor} from '@atomist/rug/operations/Decorators'
        |
        |import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
        |
        |@Editor("UpdatePropertyEditor", "Updates properties")
        |class UpdateProperty {
        |
        | edit(project: Project) {
        |
        |   project.context().pathExpressionEngine().with<Pom>(project, `/Pom()`, p => {
        |     console.log(`Pom is $${p}`)
        |     if (p.path() == "pom.xml")
        |       p.setGroupId("mygroup")
        |   })
        | }
        |
        |}
        |
        |export let pe = new UpdateProperty()
      """.stripMargin
    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case success: SuccessfulModification =>
      case _ => ???
    }
  }

  it should "add a new dependency" in {
    val prog =
      """
        |editor PomEdit
        |
        |with Pom p when path = "pom.xml"
        |  do addOrReplaceDependency "mygroup" "myartifact"
      """.stripMargin

    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case success: SuccessfulModification =>
      case _ => ???
    }
  }

}

package com.atomist.rug.kind.java.spring

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.SuccessfulModification
import org.scalatest.{FlatSpec, Matchers}

class ApplicationYmlKeyAddingEditorTest extends FlatSpec with Matchers {

  import ApplicationPropertiesToApplicationYmlEditor.ApplicationYmlPath

  "ApplicationYmlKeyAddingEditor" should "add empty application YML to project without one" in {
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    target.findFile(ApplicationYmlPath) should not be defined
    val r = ApplicationYmlKeyAddingEditor.modify(target,
      SimpleProjectOperationArguments.Empty)
    r match {
      case sma: SuccessfulModification =>
        val yamlFile = sma.result.findFile(ApplicationYmlPath)
        yamlFile should be(defined)

        yamlFile.get.content contentEquals ApplicationYmlKeyAddingEditor.YamlHeader should be(true)
      case _ => fail("Failed")
    }
  }

  val townPropertyName = "town"
  val townPropertyValue = "Tunbridge Wells"

  it should "add application YML to project without one and add a parameter" in {
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    target.findFile(ApplicationYmlPath) should not be defined
    val r = ApplicationYmlKeyAddingEditor.modify(target,
      SimpleProjectOperationArguments.singleParam("name", townPropertyName, townPropertyValue))
    r match {
      case sma: SuccessfulModification =>
        val yamlFile = sma.result.findFile(ApplicationYmlPath)
        yamlFile should be(defined)

        val content = yamlFile.get.content
        content.contains(s"\n$townPropertyName: $townPropertyValue\n") should be(true)
      case _ => fail("Failed")
    }
  }

  val homeScopedPropertyName = "home"
  val scopeSeparator = "."

  it should "add application YML to project without one and add a period-scoped parameter" in {
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    target.findFile(ApplicationYmlPath) should not be defined
    val r = ApplicationYmlKeyAddingEditor.modify(target,
      SimpleProjectOperationArguments.singleParam("name", s"$homeScopedPropertyName$scopeSeparator$townPropertyName", townPropertyValue))
    r match {
      case sma: SuccessfulModification =>
        val yamlFile = sma.result.findFile(ApplicationYmlPath)
        yamlFile should be(defined)

        val content = yamlFile.get.content
        content.contains(s"\n$homeScopedPropertyName:\n  $townPropertyName: $townPropertyValue\n") should be(true)
      case _ => fail("Failed")
    }
  }
}

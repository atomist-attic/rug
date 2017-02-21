package com.atomist.rug.kind.java.spring

import com.atomist.param.SimpleParameterValues
import com.atomist.parse.java.ParsingTargets
import com.atomist.project.edit.SuccessfulModification
import org.scalatest.{FlatSpec, Matchers}

class ApplicationYamlKeyAddingEditorTest extends FlatSpec with Matchers {

  import ApplicationPropertiesToApplicationYamlEditor.ApplicationYamlPath

  "ApplicationYamlKeyAddingEditor" should "add empty application YAML to project without one" in {
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    target.findFile(ApplicationYamlPath) should not be defined
    val r = ApplicationYamlKeyAddingEditor.modify(target,
      SimpleParameterValues.Empty)
    r match {
      case sma: SuccessfulModification =>
        val yamlFile = sma.result.findFile(ApplicationYamlPath)
        yamlFile should be(defined)

        yamlFile.get.content contentEquals ApplicationYamlKeyAddingEditor.YamlHeader.replaceAll("\\n",System.lineSeparator()) should be(true)
      case _ => fail("Failed")
    }
  }

  val townPropertyName = "town"
  val townPropertyValue = "Tunbridge Wells"

  it should "add application YAML to project without one and add a parameter" in {
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    target.findFile(ApplicationYamlPath) should not be defined
    val r = ApplicationYamlKeyAddingEditor.modify(target,
      SimpleParameterValues(townPropertyName, townPropertyValue))
    r match {
      case sma: SuccessfulModification =>
        val yamlFile = sma.result.findFile(ApplicationYamlPath)
        yamlFile should be(defined)

        val content = yamlFile.get.content
        content.contains(s"${System.lineSeparator()}$townPropertyName: $townPropertyValue${System.lineSeparator()}") should be(true)
      case _ => fail("Failed")
    }
  }

  val homeScopedPropertyName = "home"
  val scopeSeparator = "."

  it should "add application YAML to project without one and add a period-scoped parameter" in {
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    target.findFile(ApplicationYamlPath) should not be defined
    val r = ApplicationYamlKeyAddingEditor.modify(target,
      SimpleParameterValues(s"$homeScopedPropertyName$scopeSeparator$townPropertyName", townPropertyValue))
    r match {
      case sma: SuccessfulModification =>
        val yamlFile = sma.result.findFile(ApplicationYamlPath)
        yamlFile should be(defined)

        val content = yamlFile.get.content
        content.contains(s"${System.lineSeparator()}$homeScopedPropertyName:${System.lineSeparator()}  $townPropertyName: $townPropertyValue${System.lineSeparator()}") should be(true)
      case _ => fail("Failed")
    }
  }
}

package com.atomist.util.yaml

import java.util
import java.util.Properties

import org.scalatest.{FlatSpec, Matchers}

class PropertiesToMapStructureParserTest extends FlatSpec with Matchers {

  import PropertiesToMapStructureParser._
  import YamlTestUtils._

  "YamlUtils" should "construct a valid structure for a valid single period-scoped property" in {
    val properties = new Properties
    properties.put(namePropertyKeyPath, namePropertyValue)
    val resultingYamlMap = constructYamlMapForProperties(properties)
    val springEntry = resultingYamlMap.get(springPropertyPathElement)
    val applicationEntry = springEntry.asInstanceOf[util.HashMap[String, Object]].get(applicationPropertyPathElement)
    val name = applicationEntry.asInstanceOf[util.HashMap[String, Object]].get(namePropertyPathElement)
    assertResult(namePropertyValue)(name)
  }

  "YamlUtils" should "construct a valid structure for a couple of nested period-scoped properties" in {
    val properties = new Properties
    properties.put(namePropertyKeyPath, namePropertyValue)
    properties.put(idPropertyKeyPath, idPropertyValue)
    val resultingYamlMap = constructYamlMapForProperties(properties)
    val springEntry = resultingYamlMap.get(springPropertyPathElement)
    val applicationEntry = springEntry.asInstanceOf[util.HashMap[String, Object]].get(applicationPropertyPathElement)
    val name = applicationEntry.asInstanceOf[util.HashMap[String, Object]].get(namePropertyPathElement)
    assertResult(namePropertyValue)(name)
    val id = applicationEntry.asInstanceOf[util.HashMap[String, Object]].get("id")
    assertResult(idPropertyValue)(id)
  }

  "YamlUtils" should "construct a valid structure for no period-scoped properties at all" in {
    val properties = new Properties
    properties.put(singleUnNestedPropertyKey, singleUnNestedPropertyValue)
    val resultingYamlMap = constructYamlMapForProperties(properties)
    val singlePropertyEntryValue = resultingYamlMap.get(singleUnNestedPropertyKey)
    assertResult(singleUnNestedPropertyValue)(singlePropertyEntryValue)
  }
}

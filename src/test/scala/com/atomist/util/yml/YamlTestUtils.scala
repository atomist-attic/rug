package com.atomist.util.yml

import java.util
import java.util.Properties

object YamlTestUtils {

  val springPropertyPathElement = "spring"
  val applicationPropertyPathElement = "application"
  val namePropertyPathElement = "name"
  val idPropertyPathElement = "id"
  val namePropertyKeyPath = springPropertyPathElement + "." + applicationPropertyPathElement + "." + namePropertyPathElement
  val namePropertyValue = "testApp"
  val idPropertyKeyPath = springPropertyPathElement + "." + applicationPropertyPathElement + "." + idPropertyPathElement
  val idPropertyValue = "tester"
  val singleUnNestedPropertyKey = "myProperty"
  val singleUnNestedPropertyValue = "myPropertyValue"
  val emptyPropertyKeyOrValue = ""
  val validKeyEmptyValueYamlStringResult = "myProperty: \"\""
  val emptyYamlStringResult = ""
  val validNestedSingleKeyAndValueStringResult =
    """spring:
      |  application:
      |    name: testApp
      |""".stripMargin
  val validNestedMultipleKeysAndValuesStringResult =
    """spring:
      |  application:
      |    name: testApp
      |    id: tester
      |""".stripMargin
  val validKeyValidValueNotNestedResult =
    """myProperty: myPropertyValue
      |""".stripMargin

  import PropertiesToMapStructureParser._

  def constructSingleNestedPropertyMap: util.HashMap[String, Object] = {
    val properties = new Properties
    properties.put(namePropertyKeyPath, namePropertyValue)
    constructYamlMapForProperties(properties)
  }

  def constructMultipleNestedPropertyMap: util.HashMap[String, Object] = {
    val properties = new Properties
    properties.put(namePropertyKeyPath, namePropertyValue)
    properties.put(idPropertyKeyPath, idPropertyValue)
    constructYamlMapForProperties(properties)
  }

  def constructSingleUnnestedPropertyMap: util.HashMap[String, Object] = {
    val properties = new Properties
    properties.put(singleUnNestedPropertyKey, singleUnNestedPropertyValue)
    constructYamlMapForProperties(properties)
  }
}

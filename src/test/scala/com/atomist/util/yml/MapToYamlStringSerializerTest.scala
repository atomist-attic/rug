package com.atomist.util.yml

import java.util
import java.util.Properties

import org.scalatest.{FlatSpec, Matchers}

class MapToYamlStringSerializerTest extends FlatSpec with Matchers {

  import MapToYamlStringSerializer._
  import PropertiesToMapStructureParser._
  import YamlTestUtils._

  "MapToYamlStringSerializer" should "return a valid string version of the nested yaml produced from parsing period-scoped property" in {
    val yamlMap: util.HashMap[String, Object] = constructSingleNestedPropertyMap
    val result = toYamlString(yamlMap)
    assertResult(validNestedSingleKeyAndValueStringResult)(result.replaceAll("\\n",System.lineSeparator()))
  }

  // TODO Change up 'given' blocks into test util methods
  it should "return a valid string version of the nested yaml produced from parsing two period-scoped properties" in {
    val yamlMap: util.HashMap[String, Object] = constructMultipleNestedPropertyMap
    val result = toYamlString(yamlMap)
    assertResult(validNestedMultipleKeysAndValuesStringResult)(result.replaceAll("\\n",System.lineSeparator()))
  }

  it should "return a valid string version of the un-nested yaml produced from parsing two un-nested properties" in {
    val yamlMap: util.HashMap[String, Object] = constructSingleUnnestedPropertyMap
    val result = toYamlString(yamlMap)
    assertResult(validKeyValidValueNotNestedResult)(result.replaceAll("\\n",System.lineSeparator()))
  }

  it should "return an empty string version of the nested yaml produced from parsing no properties" in {
    val properties = new Properties
    val yamlMap = constructYamlMapForProperties(properties)
    val result = toYamlString(yamlMap)
    assertResult(emptyYamlStringResult)(result.replaceAll("\\n",System.lineSeparator()))
  }

  it should "return an empty string version of the nested yaml produced from parsing an empty property key and value" in {
    val properties = new Properties
    properties.put(emptyPropertyKeyOrValue, emptyPropertyKeyOrValue)
    val yamlMap = constructYamlMapForProperties(properties)
    val result = toYamlString(yamlMap)
    assertResult(emptyYamlStringResult)(result.replaceAll("\\n",System.lineSeparator()))
  }

  it should "return an empty property in the nested yaml produced from parsing a property key and empty value" in {
    val properties = new Properties
    properties.put(singleUnNestedPropertyKey, emptyPropertyKeyOrValue)
    val yamlMap = constructYamlMapForProperties(properties)
    val result = toYamlString(yamlMap)
    assertResult(validKeyEmptyValueYamlStringResult)(result.replaceAll("\\n",System.lineSeparator()))
  }
}

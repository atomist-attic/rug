package com.atomist.util.yml

import java.util
import java.util.Properties

object PropertiesToMapStructureParser {

  def constructYamlMapForProperties(properties: Properties): util.HashMap[String, Object] = {
    val yamlMap = new util.HashMap[String, Object]
    val e = properties.propertyNames

    while (e.hasMoreElements) {
      val key = e.nextElement
      val value = properties.getProperty(key.asInstanceOf[String])
      populateYamlForPeriodScopedProperty(key.asInstanceOf[String], value.asInstanceOf[String], yamlMap)
    }

    yamlMap
  }

  def populateYamlForPeriodScopedProperty(property: String, propertyValue: String, currentMap: util.HashMap[String, Object]): Unit = {
    val propertyTokens = property.split("\\.")
    constructYamlForPeriodScopedProperty(0, propertyTokens, currentMap, propertyValue)
  }

  private def constructYamlForPeriodScopedProperty(index: Int, propertyTokens: Array[String], currentMap: util.HashMap[String, Object], propertyValue: Object) {
    if (index < propertyTokens.length) {
      val currentToken = propertyTokens(index)
      if (index < propertyTokens.length - 1) {
        val nextMap = new util.HashMap[String, Object]()
        currentMap.putIfAbsent(currentToken, nextMap)
        val nextResultingMap = currentMap.get(currentToken)
        constructYamlForPeriodScopedProperty(index + 1, propertyTokens, nextResultingMap.asInstanceOf[util.HashMap[String, Object]], propertyValue)
      } else {
        currentMap.putIfAbsent(currentToken, propertyValue)
      }
    }
  }
}

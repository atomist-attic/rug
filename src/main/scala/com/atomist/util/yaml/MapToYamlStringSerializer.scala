package com.atomist.util.yaml

import java.util

object MapToYamlStringSerializer {

  val indentCharacters = "  "

  def toYamlString(yamlMap: util.HashMap[String, Object]): String = {
    val currentString = new StringBuilder
    outputYamlString(yamlMap, 0, currentString)
  }

  private def outputYamlString(currentYamlMap: util.HashMap[String, Object], indentCount: Int, currentString: StringBuilder): String = {
    val entrySet = currentYamlMap.entrySet.iterator

    while (entrySet.hasNext) {
      val entry = entrySet.next

      currentString.append(produceNumberOfIndents(indentCount, 0, new StringBuilder))
      currentString.append(entry.getKey)

      if (entry.getKey != "") {
        currentString.append(":")
      }

      val buildingString = entry.getValue match {
        case value: util.HashMap[String@unchecked, Object@unchecked] => outputYamlString(value, indentCount + 1, currentString.append("\n"))
        case "" => if (entry.getKey != "") {
          currentString.append(" ").append("\"\"")
        }
        case value => currentString.append(" ").append(value).append("\n")
      }
    }

    currentString.toString
  }

  private def produceNumberOfIndents(numberOfIndents: Int, count: Int, currentString: StringBuilder): StringBuilder = {
    if (count < numberOfIndents) {
      currentString ++= indentCharacters
      produceNumberOfIndents(numberOfIndents, count + 1, currentString)
    }
    currentString
  }
}

package com.atomist.rug.kind.properties

import java.io.StringReader
import java.util.Properties

import com.atomist.rug.kind.core.{LazyFileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription, TerminalView}
import com.atomist.source.FileArtifact

import scala.collection.JavaConverters._

class PropertiesMutableView(
                             originalBackingObject: FileArtifact,
                             parent: ProjectMutableView)
  extends LazyFileArtifactBackedMutableView(originalBackingObject, parent)
    with TerminalView[FileArtifact] {

  private var properties = originalBackingObject.content

  override protected def currentContent: String = properties

  @ExportFunction(readOnly = true, description = "Return the content of this property")
  def getValue(@ExportFunctionParameterDescription(name = "key",
    description = "The name of the simple node")
               key: String): String = {
    val regexp = s"$key=(.*)".r

    val matched = regexp.findFirstMatchIn(properties)
    if (matched.isDefined) {
      val rm = regexp.findFirstMatchIn(properties).get
      rm.group(1)
    } else {
      ""
    }
  }

  @ExportFunction(readOnly = false, description = "Set the value of the specified property, creating a property if not present")
  def setProperty(@ExportFunctionParameterDescription(name = "key",
    description = "The key of the property being set")
                  key: String,
                  @ExportFunctionParameterDescription(name = "value",
                    description = "The value of the property")
                  newValue: String
                 ): Unit = {

    if (content.contains(key)) {
      val regexp = s"$key=(.*)"
      this.properties = content.replaceFirst(regexp, s"$key=$newValue")
    } else {
      val newPropertyString = s"$key=$newValue\n"
      this.properties = content.concat(newPropertyString)
    }
  }

  @ExportFunction(readOnly = true, description = "Return whether a property key exists in this file or not")
  def containsKey(@ExportFunctionParameterDescription(name = "key",
    description = "The key of the property being searched for")
                  key: String): Boolean = {
    val properties = new Properties()
    properties.load(new StringReader(content))
    properties.containsKey(key)
  }

  @ExportFunction(readOnly = true, description = "Return whether a property value exists in this file or not")
  def containsValue(@ExportFunctionParameterDescription(name = "value",
    description = "The value being searched for")
                    value: String): Boolean = {
    val properties = new Properties()
    properties.load(new StringReader(content))
    properties.containsValue(value)
  }

  @ExportFunction(readOnly = true, description = "Return a list of the supported keys")
  def keys: List[Any] = {
    val properties = new Properties()
    properties.load(new StringReader(content))
    properties.propertyNames().asScala.toList
  }
}

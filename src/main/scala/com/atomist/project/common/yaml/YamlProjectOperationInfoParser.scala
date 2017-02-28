package com.atomist.project.common.yaml

import java.util.regex.{Pattern, PatternSyntaxException}

import com.atomist.param._
import com.atomist.project.common.template.{InvalidTemplateException, TemplateBasedProjectOperationInfo}
import com.atomist.rug.runtime.{AddressableRug, Rug}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.apache.commons.lang3.builder.ReflectionToStringBuilder

import scala.util.{Failure, Success, Try}

/**
  * Parse YAML file to return ProjectOperationInfo.
  */
object YamlProjectOperationInfoParser {

  private val mapper = new ObjectMapper(new YAMLFactory()) with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  @throws[InvalidYamlDescriptorException]
  def parse(yaml: String): TemplateBasedProjectOperationInfo = {
    if (yaml == null || "".equals(yaml))
      throw new InvalidYamlDescriptorException("YAML content required in template metadata file")

    Try(mapper.readValue(yaml, classOf[BoundProjectOperationInfo])) match {
      case s: Success[BoundProjectOperationInfo] =>
        val badPatterns = s.value.parameters.flatMap(p => patternError(p))
        if (badPatterns.nonEmpty)
          throw new InvalidYamlDescriptorException(s"Bad regexp patterns: ${badPatterns.mkString(",")}")
        s.value
      case f: Failure[BoundProjectOperationInfo] =>
        throw new InvalidYamlDescriptorException(s"Failed to parse YAML [$yaml]: ${f.exception.getMessage}", f.exception)
    }
  }

  private def patternError(p: Parameter): Option[String] = {
    try {
      Pattern.compile(p.getPattern)
      None
    } catch {
      case pse: PatternSyntaxException => Some(s"${p.getName}: Bad regular expression pattern: ${pse.getMessage}")
    }
  }
}

private class BoundProjectOperationInfo extends TemplateBasedProjectOperationInfo {

  @JsonProperty("name")
  var name: String = _

  @JsonProperty("description")
  var description: String = _

  @JsonProperty("template_name")
  var templateName: String = _

  @JsonProperty("type")
  var _templateType: String = _

  override def templateType: Option[String] =
    if (_templateType == null || "".equals(_templateType)) None
    else Some(_templateType)

  @JsonProperty("parameters")
  private var _params: Seq[Parameter] = Nil

  @JsonProperty("tags")
  private var _tags: Seq[TagHolder] = Nil

  override def parameters: Seq[Parameter] = _params

  override def tags: Seq[Tag] = _tags.map(tw => tw.toTag)

  override def toString = ReflectionToStringBuilder.toString(this)
}

private class TagHolder {

  @JsonProperty
  var name: String = _

  @JsonProperty
  var description: String = _

  def toTag = Tag(name, description)
}

class InvalidYamlDescriptorException(msg: String, ex: Throwable = null) extends InvalidTemplateException(msg, ex)

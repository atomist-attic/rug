package com.atomist.param

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonNode}

trait ParameterValue {

  def getName: String

  def getValue: AnyRef
}

object ParameterValueDeserializer extends JsonDeserializer[ParameterValue] {

  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): ParameterValue = {
    val node: JsonNode = jsonParser.getCodec.readTree(jsonParser)

    val name = node.get("name").asText
    if (name == null) throw new IllegalArgumentException("Expected 'name' parameter of TemplateParameterValue")

    val value = node.get("value").asText
    if (value == null) throw new IllegalArgumentException("Expected 'value' parameter of TemplateParameterValue")

    SimpleParameterValue(name, value)
  }
}

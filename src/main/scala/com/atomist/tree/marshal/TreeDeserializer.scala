package com.atomist.tree.marshal

import java.util.Collections

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

object TreeDeserializer {

  def fromJson(json: String): List[Map[String, Object]] = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val node = mapper.readValue(json, classOf[List[Map[String, Object]]])
    println(mapper.writeValueAsString(node))
    node
  }
}

/**
  * Intermediate representation of parsed JSON
  */
class DeserializedNode {

  private var properties: Map[String, Any] = Map()

  @JsonAnySetter
  def set(name: String, value: Any) {
    println(s"Set $name to $value")
    properties += (name -> value)
  }

  var `type`: java.util.List[String] = Collections.emptyList()
}
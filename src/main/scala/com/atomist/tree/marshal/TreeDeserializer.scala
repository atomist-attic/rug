package com.atomist.tree.marshal

import java.util
import java.util.Collections

import com.atomist.tree.TreeNode
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{DeserializationFeature, JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object TreeDeserializer {

  def fromJson(json: String): TreeNode = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    //mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val node = mapper.readValue(json, new TypeReference[java.util.Map[String, Object]] {})

    System.out.println(mapper.writeValueAsString(node))
    null
  }

}


/**
  * Intermediate representation of parsed JSON
  */
class DeserializedNode {

  private var properties: Map[String,Any] = Map()

  @JsonAnySetter
  def set(name: String, value: Any) {
    println(s"Set $name to $value")
    properties += (name -> value)
  }

  var `type`: java.util.List[String] = Collections.emptyList()

}
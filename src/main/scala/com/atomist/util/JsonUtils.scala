package com.atomist.util

import java.io.InputStream
import java.text.SimpleDateFormat

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

/**
  * Serialize objects to Json.
  */
object JsonUtils {

  private val mapper = getObjectMapper

  private val wrapper = getObjectMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true)

  // Wrap should be the default to avoid breaking a bunch of stuff outside rug that use it!
  def toJson(value: Any): String = mapper.writeValueAsString(value)

  def toJson(ref: Option[AnyRef]): Option[String] =
    if (ref.isDefined) Some(toJson(ref.get)) else None

  def toWrappedJson(value: Any): String = wrapper.writeValueAsString(value)

  def toJsonPrettyPrint(value: Any): String = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(value)

  def fromJson[T](json: String)(implicit m: Manifest[T]): T = mapper.readValue[T](json)

  def fromJson[T](json: String, clazz: Class[T]): T = mapper.readValue(json, clazz)

  def fromJson[T](is: InputStream)(implicit m: Manifest[T]): T = mapper.readValue[T](is)

  private def getObjectMapper = {
    val objectMapper = new ObjectMapper() with ScalaObjectMapper
    objectMapper.registerModule(DefaultScalaModule)
      .registerModule(new JavaTimeModule())
      .registerModule(new Jdk8Module())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, SerializationFeature.INDENT_OUTPUT)
      .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .setSerializationInclusion(Include.NON_NULL)
      .setSerializationInclusion(Include.NON_ABSENT)
      .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"))
    objectMapper
  }
}

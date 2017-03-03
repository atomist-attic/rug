package com.atomist.rug.runtime.js


import java.text.SimpleDateFormat

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

/**
  * Serialize objects to Json
  */
object JsonSerializer {

  private val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
    .registerModule(new JavaTimeModule())
    .registerModule(new Jdk8Module())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, SerializationFeature.INDENT_OUTPUT)
    .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .setSerializationInclusion(Include.NON_NULL)
    .setSerializationInclusion(Include.NON_ABSENT)
    .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"))

  def toJson(ref: Option[AnyRef]): Option[String] = {
    if(ref.nonEmpty){
      Some(toJsonInternal(ref.get))
    }else{
      None
    }
  }

  def toJson(ref: AnyRef): String = {
   toJsonInternal(ref)
  }

  private def toJsonInternal(ref: AnyRef): String = {
    mapper.writeValueAsString(ref)
  }
}

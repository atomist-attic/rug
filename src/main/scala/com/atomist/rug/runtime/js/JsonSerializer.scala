package com.atomist.rug.runtime.js

import java.io.StringWriter

import com.fasterxml.jackson.databind.{ObjectMapper, ObjectWriter, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Serialize nashorn objects to Json
  */
object JsonSerializer {

  // Configure this to handle Scala
  private val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true)

  private val objectWriter: ObjectWriter = mapper.writer()

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
    val writer = new StringWriter()
    objectWriter.writeValue(writer, ref)
    val str = writer.toString
    str.substring(22).dropRight(1)
  }
}

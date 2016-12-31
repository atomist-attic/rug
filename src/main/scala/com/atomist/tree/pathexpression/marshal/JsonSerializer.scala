package com.atomist.tree.pathexpression.marshal

import java.io.StringWriter

import com.atomist.tree.pathexpression.PathExpression
import com.fasterxml.jackson.databind.{ObjectMapper, ObjectWriter, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

/**
  * Serialize path expressions to JSON
  */
object JsonSerializer {

  // Configure this to handle Scala
  private val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true)

  private val objectWriter: ObjectWriter = mapper.writer().withDefaultPrettyPrinter()

  def toJson(pe: PathExpression): String = {
    val writer = new StringWriter()
    objectWriter.writeValue(writer, pe)
    writer.toString
  }

}

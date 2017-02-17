package com.atomist.rug.spi

import java.util.Collections

import com.atomist.rug.spi.Handlers.Instruction.{Detail, Edit, Generate}
import com.atomist.rug.spi.Handlers.Status.{Failure, Success}
import org.scalatest._
import com.atomist.rug.spi.JavaHandlersConverter._
import com.atomist.rug.spi.JavaHandlers.{Instruction => JavaInstruction, Message => JavaMessage, Response => JavaResponse}
import com.atomist.rug.spi.Handlers.{MavenCoordinate, MessageText, Presentable, Instruction => ScalaInstruction, Message => ScalaMessage, Response => ScalaResponse}
import java.util.{List => JList}

class JavaHandlersConverterTest extends FunSpec with Matchers with DiagrammedAssertions with OneInstancePerTest  {

  it ("should convert minimal Java Response to Scala Response") {
    val javaResponse = JavaResponse(Success.toString, null, 0, null)
    val scalaResponse = ScalaResponse(Success, None, Some(0), None)
    assert(toScalaResponse(javaResponse) == scalaResponse)
  }

  it ("should convert Java Response with missing status to Failure Scala Response") {
    val javaResponse = JavaResponse(null, null, 0, null)
    val scalaResponse = ScalaResponse(Failure, None, Some(0), None)
    assert(toScalaResponse(javaResponse) == scalaResponse)
  }

  it ("should convert Java Response to Scala Response") {
    val javaResponse = JavaResponse("failure", "message1", 42, "body")
    val scalaResponse = ScalaResponse(Failure, Some("message1"), Some(42), Some("body"))
    assert(toScalaResponse(javaResponse) == scalaResponse)
  }

  it ("should convert minimal Scala Instruction to Java Instruction") {
    val scalaInstruction = Edit(Detail("edit1", None, Nil))
    val javaInstruction = JavaInstruction(
      "Edit",
      "edit1",
      null,
      Collections.emptyList()
    )
    assert(toJavaInstruction(scalaInstruction) == javaInstruction)
  }

  it ("should convert Scala Instruction to Java Instruction") {
    val scalaInstruction = Edit(Detail("edit1", Some(MavenCoordinate("g", "a", Some("v"))), Nil))
    val javaInstruction = JavaInstruction(
      "Edit",
      "edit1",
      JavaHandlers.MavenCoordinate("g", "a", "v"),
      Collections.emptyList()
    )
    assert(toJavaInstruction(scalaInstruction) == javaInstruction)
  }

  it ("should convert Scala Instruction with latest Maven Coordinate to Java Instruction") {
    val scalaInstruction = Edit(Detail("edit1", Some(MavenCoordinate("g", "a", None)), Nil))
    val javaInstruction = JavaInstruction(
      "Edit",
      "edit1",
      JavaHandlers.MavenCoordinate("g", "a", null),
      Collections.emptyList()
    )
    assert(toJavaInstruction(scalaInstruction) == javaInstruction)
  }

  it ("should convert minimal Scala Message to Java Message") {
    val scalaMessage = ScalaMessage(MessageText("message1"), Nil, None)
    val javaMessage = JavaMessage(MessageText("message1"), Collections.emptyList(), null)
    assert(toJavaMessage(scalaMessage) == javaMessage)
  }

  it ("should convert Scala Message to Java Message") {
    val scalaMessage = ScalaMessage(MessageText("message1"),
      Seq(Presentable(Generate(Detail("generate1", None, Nil)), Some("label1"))),
      Some("channel1"))
    val instructionJList: JList[JavaHandlers.Presentable] = new java.util.ArrayList()
    instructionJList.add(JavaHandlers.Presentable(
      JavaInstruction(
        "Generate",
        "generate1",
        null,
        Collections.emptyList()
      ),
      "label1"
    ))
    val javaMessage = JavaMessage(MessageText("message1"),
      instructionJList,
      "channel1")
    assert(toJavaMessage(scalaMessage) == javaMessage)
  }

}

package com.atomist.rug.spi

import com.atomist.rug.spi.JavaHandlers.{Instruction => JavaInstruction, Message => JavaMessage, Response => JavaResponse}
import com.atomist.rug.spi.Handlers.{Status, Instruction => ScalaInstruction, Message => ScalaMessage, Response => ScalaResponse}

import scala.collection.JavaConverters._
import java.util.{List => JList}

import com.atomist.rug.spi.Handlers.Status.Failure

object JavaHandlersConverter {

  def toScalaResponse(response: JavaResponse): ScalaResponse = {
    val status = Option(response.status) match {
      case Some(s) => Status.from(s.toLowerCase())
      case _ => Failure
    }
    ScalaResponse(
      status,
      Option(response.msg),
      Option(response.code),
      Option(response.body)
    )
  }

  def toJavaMessage(response: ScalaMessage): JavaMessage = {
    val javaInstructions: JList[JavaHandlers.Presentable] = response.instructions.map( p => JavaHandlers.Presentable(
      toJavaInstruction(p.instruction),
      p.label.orNull
    )).toBuffer.asJava
    JavaMessage(
      response.body,
      javaInstructions,
      response.channelId.orNull
    )
  }

  def toJavaInstruction(instruction: ScalaInstruction): JavaInstruction = {
    val coordinates: JavaHandlers.MavenCoordinate = instruction.detail.coordinates match {
      case Some(m) => JavaHandlers.MavenCoordinate(m.group, m.artifact, m.version.orNull)
      case _ => null
    }
    val kind = instruction.getClass.getName.split('$').last
    JavaInstruction(
      kind,
      instruction.detail.name,
      coordinates,
      instruction.detail.parameters.toBuffer.asJava
    )
  }

}

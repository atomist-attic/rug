package com.atomist.rug.spi

import com.atomist.param.ParameterValue
import com.atomist.rug.spi.Handlers.MessageBody
import java.util.{List => JList}

/**
  * Beans that map to the @atomist/rug/operations/operations/Handlers
  * These are Java friendly.
  */
object JavaHandlers {

  case class Message(body: MessageBody,
                     instructions: JList[Presentable],
                     channelId: String)

  case class Presentable(
                          instruction: Instruction,
                          label: String
                        )

  case class Instruction(
      kind: String,
      name: String,
      coordinates: MavenCoordinate,
      parameters: JList[ParameterValue]
   )

  case class MavenCoordinate(group: String,
                           artifact: String,
                           version: String)

  case class Response(status: String,
                      msg: String,
                      code: Int,
                      body: AnyRef)

}

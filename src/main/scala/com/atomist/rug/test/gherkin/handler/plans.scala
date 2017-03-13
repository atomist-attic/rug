package com.atomist.rug.test.gherkin.handler

import com.atomist.rug.spi.Handlers.{Message, Plan}

import scala.collection.JavaConverters._

/**
  * JavaScript-friendly version of Plan structure, without Scala collections and using null instead of Option
  */
class jsPlan(plan: Plan) {

  def messages: java.util.List[jsMessage] =
    plan.messages.map(new jsMessage(_)).asJava

}

class jsMessage(message: Message) {

  def body = message.body

}


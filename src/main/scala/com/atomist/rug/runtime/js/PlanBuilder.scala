package com.atomist.rug.runtime.js

import com.atomist.param.{ParameterValue, SimpleParameterValue}
import com.atomist.rug.InvalidHandlerResultException
import com.atomist.rug.spi.Handlers.Instruction.Respond
import com.atomist.rug.spi.Handlers._
import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.Undefined

/**
  * Constructs plans from Nashorn response to a Handler/handle operation
  */
object ConstructPlan {
  def apply(jsObj: Any): Option[Plan] = {
    jsObj match {
      case o: ScriptObjectMirror => Some(new PlanBuilder().constructPlan(o))
      case other => throw new InvalidHandlerResultException(s"Could not construct Plan from: $other")
    }
  }
}

class PlanBuilder {

  def constructPlan(jsPlan: ScriptObjectMirror): Plan = {
    val jsMessages = jsPlan.getMember("messages") match {
      case o: ScriptObjectMirror => o.values().toArray.toList
      case _ => Nil
    }
    val messages: Seq[Message] = jsMessages.map { message =>
      val m = message.asInstanceOf[ScriptObjectMirror]
      constructMessage(m)
    }
    val instructions = jsPlan.getMember("instructions") match {
      case u: Undefined => Nil
      case jsInstructions: ScriptObjectMirror =>
        jsInstructions.values().toArray.toList.map { respondable =>
          constructRespondable(respondable.asInstanceOf[ScriptObjectMirror])
        }
    }
    //we are allowed to return Messages directly from handlers!
    if(messages.isEmpty && instructions.isEmpty){
      Plan(Seq(constructMessage(jsPlan)),Nil)
    }else{
      Plan(messages, instructions)
    }
  }

  def constructMessage(jsMessage: ScriptObjectMirror): Message = {
    val instructions = jsMessage.getMember("instructions") match {
      case _: Undefined => Nil
      case jsInstructions: ScriptObjectMirror =>
        jsInstructions.values().toArray.toList.map { presentable =>
          constructPresentable(presentable.asInstanceOf[ScriptObjectMirror])
        }
    }
    val messageBody = (jsMessage.getMember("body"), jsMessage.getMember("text")) match {
      case (json: ScriptObjectMirror, _) =>
        JsonBody(json.entrySet().toString)
      case (_, text: String) =>
        MessageText(text)
      case _ =>
        throw new InvalidHandlerResultException(s"Cannot determine message content from body: ${jsMessage.getMember("body")} or text: ${jsMessage.getMember("text")}")
    }

    val channelId = jsMessage.getMember("channelId") match {
      case c: String => Some(c)
      case _ => None
    }
    Message(
      messageBody,
      instructions,
      channelId
    )
  }

  def constructPresentable(jsPresentable: ScriptObjectMirror) : Presentable = {
    val instruction = jsPresentable.getMember("instruction") match {
      case o: ScriptObjectMirror => constructInstruction(o)
      case x => throw new InvalidHandlerResultException(s"A Message Presentable must contain an 'Instruction', actually $x")
    }
    val label = jsPresentable.getMember("label") match {
      case x: String => Some(x)
      case _ => None
    }
    Presentable(instruction,label)
  }

  def constructRespondable(jsRespondable: ScriptObjectMirror): Respondable = {
    val instruction: Instruction = jsRespondable.getMember("instruction") match {
      case u: Undefined =>
        throw new IllegalArgumentException(s"No instruction found in $jsRespondable")
      case o: ScriptObjectMirror =>
        constructInstruction(o)
    }
    val onSuccess: Option[Callback] = constructCallback(jsRespondable.getMember("onSuccess"))
    val onError: Option[Callback] = constructCallback(jsRespondable.getMember("onError"))
    Respondable(instruction, onSuccess, onError)
  }

  def constructInstruction(jsInstruction: ScriptObjectMirror): Instruction = {
    val jsInstructionKind = jsInstruction.getMember("kind") match {
      case kind: String => kind
      case x => throw new InvalidHandlerResultException(s"An Instruction must have a valid 'kind', actually: $x")
    }
    val detail = constructInstructionDetail(jsInstruction)
    Instruction.from(jsInstructionKind, detail)
  }

  def constructInstructionDetail(jsInstruction: ScriptObjectMirror): Instruction.Detail = {
      val parameters: Seq[ParameterValue] = jsInstruction.getMember("parameters") match {
        case u: Undefined => Nil
        case o: ScriptObjectMirror =>
          o.keySet().toArray.toList.map { key =>
            val name = key.asInstanceOf[String]
            val value = o.getMember(name)
            SimpleParameterValue(name, value)
          }
      }

      val (name, coordinates) = jsInstruction.getMember("name") match {
        case name: String =>
          (name, None)
        case o: ScriptObjectMirror =>
          val name = o.getMember("name").asInstanceOf[String]
          val coordinates = MavenCoordinate(
            o.getMember("group").asInstanceOf[String],
            o.getMember("artifact").asInstanceOf[String])
          (name, Some(coordinates))
      }

      val project_name = jsInstruction.getMember("project") match {
        case project_name: String => Some(project_name)
        case _ => None
      }
    Instruction.Detail(name,coordinates,parameters,project_name)
  }

  def constructCallback(callback: Object): Option[Callback] = {
    callback match {
      case u: Undefined => None
      case jsOnSuccess: ScriptObjectMirror =>
        val callback = if (jsOnSuccess.hasMember("text") || jsOnSuccess.hasMember("body")) {
          constructMessage(jsOnSuccess)
        } else if (jsOnSuccess.hasMember("kind")) {
          Respond(constructInstructionDetail(jsOnSuccess))
        } else if (jsOnSuccess.hasMember("messages")) {
          constructPlan(jsOnSuccess)
        } else {
          throw new InvalidHandlerResultException(s"Cannot create CallBack from: $jsOnSuccess")
        }
        Option(callback)
    }
  }
}

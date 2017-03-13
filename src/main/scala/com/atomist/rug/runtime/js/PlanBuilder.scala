package com.atomist.rug.runtime.js

import com.atomist.param.{ParameterValue, SimpleParameterValue}
import com.atomist.rug.InvalidHandlerResultException
import com.atomist.rug.spi.Handlers.Instruction.{NonrespondableInstruction, Respond, RespondableInstruction}
import com.atomist.rug.spi.Handlers._
import com.atomist.tree.TreeNode
import com.atomist.util.JsonUtils
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
    if (jsPlan.hasMember("body")) {
      // we are allowed to return a Message directly from handlers
      Plan(Seq(constructMessage(jsPlan)),Nil)
    } else {
      val jsMessages = jsPlan.getMember("_messages") match {
        case o: ScriptObjectMirror => o.values().toArray.toList
        case _ => Nil
      }
      val messages: Seq[Message] = jsMessages.map { message =>
        val m = message.asInstanceOf[ScriptObjectMirror]
        constructMessage(m)
      }
      val instructions = jsPlan.getMember("_instructions") match {
        case u: Undefined => Nil
        case jsInstructions: ScriptObjectMirror =>
          jsInstructions.values().toArray.toList.map { respondable =>
            constructRespondable(respondable.asInstanceOf[ScriptObjectMirror])
          }
      }
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
    val messageBody = jsMessage.getMember("body") match {
      case json: ScriptObjectMirror =>
        JsonBody(json.entrySet().toString)
      case text: String =>
        MessageText(text)
      case _ =>
        throw new InvalidHandlerResultException(s"Cannot determine message content from body: ${jsMessage.getMember("body")}")
    }

    val channelId = jsMessage.getMember("channelId") match {
      case c: String => Some(c)
      case _ => None
    }

    val correlationId = jsMessage.getMember("correlationId") match {
      case c: String => Some(c)
      case _ => None
    }

    val treeNode = jsMessage.getMember("treeNode") match {
      case t: TreeNode => Some(t)
      case _ => None
    }

    Message(
      messageBody,
      instructions,
      channelId,
      correlationId,
      treeNode
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

  def constructRespondable(jsRespondable: ScriptObjectMirror): Plannable = {
    val instruction: Instruction = jsRespondable.getMember("instruction") match {
      case u: Undefined =>
        throw new IllegalArgumentException(s"No instruction found in $jsRespondable")
      case o: ScriptObjectMirror =>
        constructInstruction(o)
    }
    val onSuccess: Option[Callback] = constructCallback(jsRespondable.getMember("onSuccess"))
    val onError: Option[Callback] = constructCallback(jsRespondable.getMember("onError"))
    instruction match {
      case nr: NonrespondableInstruction => Nonrespondable(nr)
      case r: RespondableInstruction => Respondable(r, onSuccess, onError)
    }
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
            SimpleParameterValue(name,
              value match {
                case s: String => s
                case o => JsonUtils.toJson(o)
              })
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
        val callback = if (jsOnSuccess.hasMember("body")) {
          constructMessage(jsOnSuccess)
        } else if (jsOnSuccess.hasMember("kind")) {
          Respond(constructInstructionDetail(jsOnSuccess))
        } else if (jsOnSuccess.hasMember("messages") || jsOnSuccess.hasMember("instructions")) {
          constructPlan(jsOnSuccess)
        } else {
          throw new InvalidHandlerResultException(s"Cannot create CallBack from: $jsOnSuccess")
        }
        Option(callback)
    }
  }
}




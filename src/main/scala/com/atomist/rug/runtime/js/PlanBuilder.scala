package com.atomist.rug.runtime.js

import com.atomist.graph.GraphNode
import com.atomist.param.{ParameterValue, SimpleParameterValue}
import com.atomist.rug.InvalidHandlerResultException
import com.atomist.rug.runtime.Rug
import com.atomist.rug.spi.Handlers.Instruction.{NonrespondableInstruction, Respond, RespondableInstruction}
import com.atomist.rug.spi.Handlers._
import com.atomist.util.JsonUtils
import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.{ScriptRuntime, Undefined}

/**
  * Constructs plans from Nashorn response to a Handler/handle operation
  */
object ConstructPlan {

  def apply(jsObj: Any, returningRug: Some[Rug]): Option[Plan] = {
    jsObj match {
      case o: ScriptObjectMirror => Some(new PlanBuilder().constructPlan(o, returningRug))
      case other => throw new InvalidHandlerResultException(s"Could not construct Plan from: $other")
    }
  }
}

class PlanBuilder {

  def constructPlan(jsPlan: ScriptObjectMirror, returningRug: Option[Rug]): Plan = {

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
          constructRespondable(respondable.asInstanceOf[ScriptObjectMirror], returningRug)
        }
    }

    val lifecycle = messages.collect{case o: LifecycleMessage => o}
    val local = messages.collect{case o: LocallyRenderedMessage => o}

    Plan(returningRug, lifecycle, local, instructions, nativeObject = Some(jsPlan))
  }

  def constructMessage(jsMessage: ScriptObjectMirror): Message = {

    jsMessage.getMember("kind") match {
      case "response" | "directed" =>
        val messageBody = jsMessage.getMember("body") match {
          case json: ScriptObjectMirror =>
            throw new UnsupportedOperationException("Message body must be a string")
          case text: String => text
          case _ =>
            throw new InvalidHandlerResultException(s"Cannot determine message content from body: ${jsMessage.getMember("body")}")
        }
        val contentType = jsMessage.getMember("contentType") match {
          case c: String => c
          case _ => throw new InvalidHandlerResultException(s"Message must have a `contentType`")
        }
        val usernames = jsMessage.getMember("usernames") match {
          case _: Undefined => Nil
          case usernames: ScriptObjectMirror =>
            usernames.values().toArray.toSeq.asInstanceOf[Seq[String]]
        }
        val channelNames = jsMessage.getMember("channelNames") match {
          case _: Undefined => Nil
          case channelNames: ScriptObjectMirror =>
            channelNames.values().toArray.toSeq.asInstanceOf[Seq[String]]
        }
        LocallyRenderedMessage(messageBody, contentType, channelNames, usernames)

      case "lifecycle"=>
        val instructions = jsMessage.getMember("instructions") match {
          case _: Undefined => Nil
          case jsInstructions: ScriptObjectMirror =>
            jsInstructions.values().toArray.toList.map { presentable =>
              constructPresentable(presentable.asInstanceOf[ScriptObjectMirror])
            }
        }

        val node = jsMessage.getMember("node") match {
          case t: GraphNode => t
          case a => throw new InvalidHandlerResultException(s"Lifecycle messages must contain a GraphNode, actually: $a")
        }

        val id = jsMessage.getMember("lifecycleId") match {
          case s: String => s
          case _ => throw new InvalidHandlerResultException("Lifecycle messages must contain a lifecycleId")
        }
        LifecycleMessage(node,instructions,id)

      case _: Undefined => throw new InvalidHandlerResultException(s"A message must have a kind: $jsMessage")
    }
  }

  def constructPresentable(jsPresentable: ScriptObjectMirror): Presentable = {
    val instruction = jsPresentable.getMember("instruction") match {
      case o: ScriptObjectMirror => constructInstruction(o)
      case x => throw new InvalidHandlerResultException(s"A Message Presentable must contain an 'Instruction', actually $x")
    }
    val label = jsPresentable.getMember("label") match {
      case x: String => Some(x)
      case _ => None
    }
    Presentable(instruction, label)
  }

  def constructRespondable(jsRespondable: ScriptObjectMirror, returningRug: Option[Rug]): Plannable = {
    val instruction: Instruction = jsRespondable.getMember("instruction") match {
      case u: Undefined =>
        throw new IllegalArgumentException(s"No instruction found in $jsRespondable")
      case o: ScriptObjectMirror =>
        constructInstruction(o)
    }
    val onSuccess: Option[Callback] = constructCallback(jsRespondable.getMember("onSuccess"), returningRug)
    val onError: Option[Callback] = constructCallback(jsRespondable.getMember("onError"), returningRug)
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
        val filtered = o.keySet().toArray.filter(key => {
          val name = key.asInstanceOf[String]
          o.getMember(name) match {
            case null => false
            case ScriptRuntime.UNDEFINED => false
            case _ => true
          }
        })

        filtered.map { key =>
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
    Instruction.Detail(name, coordinates, parameters, project_name)
  }

  def constructCallback(callback: Object, returningRug: Option[Rug]): Option[Callback] = {
    callback match {
      case u: Undefined => None
      case jsOnSuccess: ScriptObjectMirror =>
        val callback = jsOnSuccess.getMember("kind") match {
          case "response" | "lifecycle" | "directed" => constructMessage(jsOnSuccess)
          case _: String => Respond(constructInstructionDetail(jsOnSuccess))
          case _ =>
            if (jsOnSuccess.hasMember("messages") || jsOnSuccess.hasMember("instructions")) {
            constructPlan(jsOnSuccess, returningRug)
          }
          else {
            throw new InvalidHandlerResultException(s"Cannot create CallBack from: $jsOnSuccess")
          }
        }
        Option(callback)
    }
  }
}

package com.atomist.rug.kind.service

import java.util
import java.util.Collections

import com.atomist.tree.TreeNode

class SimpleMessageBuilder(val teamId: String,
                           sender: Message => Unit,
                           val actionRegistry: ActionRegistry)
  extends MessageBuilder {

  def regarding(n: TreeNode): Message =
    ImmutableMessage(send, actionRegistry, teamId, node = n)

  def say(msg: String): Message =
    ImmutableMessage(send, actionRegistry, teamId, message = msg)

  private def send(message: Message): Unit = sender(message)

}


case class ImmutableMessage(
                             sender: Message => Unit,
                             actionRegistry: ActionRegistry,
                             teamId: String,
                             node: TreeNode = null,
                             message: String = null,
                             address: String = null,
                             actions: java.util.List[Action] = new util.ArrayList[Action]())
  extends Message {

  // We use null for interop and JSON

  override def say(msg: String): Message = copy(message = message)

  override def withAction(action: Action): Message = {
    // Nasty Java mutable list.
    actions.add(action)
    this
  }

  override def withActionNamed(a: String): Message = {
    actions.add(actionRegistry.findByName(a))
    this
  }

  override def send(): Unit = sender(this)

  /**
    * Specify channel address. This can also be used
    * for direct messages
    *
    * @param channelId channel to address to.
    * @return updated message
    */
  def address(channelId: String): Message = copy(address = channelId)

  def on(channelId: String): Message = address(channelId)

}

object EmptyActionRegistry extends ActionRegistry {

  override def findByName(name: String): Action = null

  override def bindParameter(action: Action, name: String, value: Object) = null
}

class ConsoleMessageBuilder(teamId: String, actionRegistry: ActionRegistry)
  extends SimpleMessageBuilder(
  teamId,
  m => println(m),
  actionRegistry
)
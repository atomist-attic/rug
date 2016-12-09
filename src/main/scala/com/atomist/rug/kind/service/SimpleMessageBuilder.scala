package com.atomist.rug.kind.service

import java.util.Collections

import com.atomist.tree.TreeNode

class SimpleMessageBuilder(val teamId: String, sender: Message => Unit)
  extends MessageBuilder {

  def regarding(n: TreeNode): Message =
    ImmutableMessage(send, teamId, node = n)

  def say(msg: String): Message =
    ImmutableMessage(send, teamId, message = msg)

  private def send(message: Message): Unit = sender(message)

}


case class ImmutableMessage(
                             sender: Message => Unit,
                             teamId: String,
                             node: TreeNode = null,
                             message: String = null,
                             address: String = null,
                             actions: java.util.List[Action] = Collections.emptyList())
  extends Message {

  // We use null for interop and JSON

  override def say(msg: String): Message = copy(message = message)

  override def withAction(action: Action): Message = {
    // Nasty Java mutable list.
    actions.add(action)
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

class ConsoleMessageBuilder(teamId: String) extends SimpleMessageBuilder(
  teamId,
  m => println(m)
)
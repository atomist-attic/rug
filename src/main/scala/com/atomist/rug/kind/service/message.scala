package com.atomist.rug.kind.service

import com.atomist.tree.TreeNode

class MessageBuilder(val teamId: String, sender: Message => Unit) {

  def regarding(n: TreeNode): Message =
    Message(send, teamId, node = n)

  def say(msg: String): Message =
    Message(send, teamId, message = msg)

  private def send(message: Message): Unit = sender(message)

}

case class Message(sender: Message => Unit,
                   teamId: String,
                   node: TreeNode = null,
                   message: String = null,
                   address: String = null,
                   action: String = null) {

  // We use null for interop and JSON

  def say(msg: String): Message = copy(message = message)

  def withAction(s: String): Message = copy(action = s)

  def send(): Unit = sender(this)

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

class ConsoleMessageBuilder(teamId: String) extends MessageBuilder(
  teamId,
  m => println(m)
)
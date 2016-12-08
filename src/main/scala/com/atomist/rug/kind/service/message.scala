package com.atomist.rug.kind.service

import com.atomist.tree.TreeNode

class MessageBuilder(sender: Message => Unit) {

  def regarding(n: TreeNode, teamId: String): Message =
    Message(send, teamId, node = n)

  def say(msg: String, address: String, teamId: String): Message =
    Message(send, teamId, message = msg).address(address)

  private def send(message: Message): Unit = sender(message)

}


// We use null for interop and JSON
case class Message(sender: Message => Unit,
                   teamId: String,
                   node: TreeNode = null,
                   message: String = null,
                   address: String = null,
                   action: String = null) {

  def withAction(s: String): Message = copy(action = s)

  def send(): Unit = sender(this)

  /**
    * Specify channel address. This can also be used
    * for direct messages
    * @param channelId channel to address to.
    * @return updated message
    */
  def address(channelId: String): Message = copy(address = channelId)

}

object ConsoleMessageBuilder extends MessageBuilder(
  m => println(m)
)
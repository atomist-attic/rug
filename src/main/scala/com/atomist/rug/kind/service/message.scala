package com.atomist.rug.kind.service

import com.atomist.param.ParameterValue
import com.atomist.tree.TreeNode


/**
  * Interface for sending messages on behalf of a teah
  */
trait MessageBuilder {

  def teamId: String

  /**
    * Construct a message regarding the given tree node.
    * @param n node message concerns
    * @return new message
    */
  def regarding(n: TreeNode): Message

  /**
    * Send a message without an attached tree node.
    * Such messages should normally be addressed to a channel
    * by using the "on" method on Message.
    * @param msg
    * @return
    */
  def say(msg: String): Message

}


trait Message {

  def actionRegistry: ActionRegistry

  def teamId: String

  def node: TreeNode

  def message: String

  def address: String

  def actions: java.util.List[Action]

  def say(msg: String): Message

  def withAction(a: Action): Message

  def send(): Unit

  /**
    * Specify channel address. This can also be used
    * for direct messages
    *
    * @param channelId channel to address to.
    * @return updated message
    */
  def address(channelId: String): Message

  def on(channelId: String): Message

}


trait ActionRegistry {

  /**
    * Return the named Action or null if not found
    * @param name
    * @return
    */
  def findByName(name: String): Action

}


case class Action(title: String,
                  callback: Callback,
                  parameters: java.util.List[ParameterValue])


case class Callback(callbackType: String, rug: Rug)

case class Rug(group: String, artifact: String, name: String, version: String)

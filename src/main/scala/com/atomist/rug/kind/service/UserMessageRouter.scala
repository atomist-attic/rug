package com.atomist.rug.kind.service

/**
  * Interface for communicating to users
  */
trait UserMessageRouter {

  /**
    * Send a message to the channel associated with the current service,
    * or `general` or some other fallback channel if the service is null
    *
    * @param service service we're concerned with. Null if this affects multiple services
    * @param msg     message
    */
  def messageServiceChannel(service: Service, msg: String): Unit

  /**
    * Send a message to the current user, possibly associated with a current service.
    *
    * @param service    service we're concerned with. Null if this affects multiple services
    * @param screenName screen name in chat
    * @param msg        message
    */
  def messageUser(service: Service, screenName: String, msg: String): Unit

  /**
    * Send a message to the given channel
    *
    * @param channelName name of the channel
    * @param msg         message text
    */
  def messageChannel(channelName: String, msg: String): Unit
}

/**
  * UserMessageRouter implementation that simply routes all messages to the console
  */
object ConsoleUserMessageRouter extends UserMessageRouter {

  override def messageServiceChannel(service: Service, msg: String): Unit = println(msg)

  override def messageChannel(channel: String, msg: String): Unit = println(s"#$channel: $msg")

  override def messageUser(service: Service, screenName: String, msg: String): Unit = println(s"@$screenName: $msg")
}
package com.atomist.event

import com.atomist.param.Tag
import com.atomist.rug.kind.service.ServiceSource

/**
  * System event, such as a new issue
  *
  * @param teamId       id of the team this event is destined for
  * @param rootNodeName name of the root name. Such as Issue
  * @param id           unique id of the given root node, within the present
  *                     ServiceSource.
  */
case class SystemEvent(
                        teamId: String,
                        rootNodeName: String,
                        id: Long)

// TODO do we need whether it's created or changed?

/**
  * Event implemented by SystemEventHandlers
  */
trait SystemEventHandler {

  def name: String

  def tags: Seq[Tag]

  def description: String

  /**
    * Return the name of the the root node that this handler is interested in,
    * such as "Issue". The return value can be cached and used to avoid invoking
    * handlers unnecessarily.
    *
    * @return name of the root node we're interested in
    */
  val rootNodeName: String

  /**
    * Handle the given event type. Communication occurs back
    * through the ServiceSource.
    *
    * @param e SystemEvent we're processing
    * @param s2 ServiceSource providing context for the current team
    */
  def handle(e: SystemEvent, s2: ServiceSource): Unit
}

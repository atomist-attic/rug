package com.atomist.rug.runtime

import com.atomist.rug.runtime.js.interop.RugContext
import com.atomist.rug.spi.Handlers.Plan

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

/**
  * Event implemented by EventHandlers
  */
trait EventHandler extends Rug {

  /**
    * Return the name of the the root node that this handler is interested in,
    * such as "Issue". The return value can be cached and used to avoid invoking
    * handlers unnecessarily.
    *
    * @return name of the root node we're interested in
    */
  val rootNodeName: String

  /**
    * Handle the given event type.
    *
    * @param e SystemEvent we're processing
    */
  def handle(ctx: RugContext, e: SystemEvent): Option[Plan]
}

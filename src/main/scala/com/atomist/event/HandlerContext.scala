package com.atomist.event

import com.atomist.rug.kind.service.{ServiceSource, ServicesMutableView}

/**
  * Implemented by model elements that want to know
  * about the present context. Used in a NodePreparer
  */
trait ModelContextAware {

  private var _context: HandlerContext = _

  def setContext(hc: HandlerContext): Unit = _context = hc

  def context: ServiceSource = handlerContext.servicesMutableView.currentBackingObject

  def handlerContext: HandlerContext =
    if (_context == null)
      throw new IllegalStateException(s"Internal error: Model context not set in $this")
    else
      _context
}

/**
  * The context in which a handler invocation occurs
 *
  * @param servicesMutableView services mutable view we can use to update services
  *            or access ServiceSource
  */
case class HandlerContext(servicesMutableView: ServicesMutableView)
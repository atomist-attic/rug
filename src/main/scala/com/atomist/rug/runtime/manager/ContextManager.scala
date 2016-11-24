package com.atomist.rug.runtime.manager

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.service.ServicesMutableView

/**
  * Context for the current user interaction
  */
trait DataContext {

  def services: ServicesMutableView

  def userContext: UserContext
}

case class SimpleDataContext(services: ServicesMutableView, userContext: UserContext)
  extends DataContext

trait ContextManager {

  @throws[RugRuntimeException]
  def context(): DataContext
}

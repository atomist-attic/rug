package com.atomist.rug.runtime.manager

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.service.ServiceSource

/**
  * Creates ServiceSource from user context.
  */
trait ServiceSourceCreator {

  @throws[RugRuntimeException]
  def serviceSourceFor(uc: UserContext): ServiceSource
}

class DefaultServiceSourceCreator extends ServiceSourceCreator {

  override def serviceSourceFor(uc: UserContext): ServiceSource = uc match {
    case fuc: FakeUserContext => fuc.serviceSource
    case ghuc: GitHubUserContext => ???
    case _ => throw new RugRuntimeException(null, s"Cannot create ServiceService for $uc")
  }

}

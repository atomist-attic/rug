package com.atomist.rug.runtime.manager

import com.atomist.rug.kind.service.ServiceSource

trait UserContext

case class GitHubUserContext(
                              org: String,
                              gitHubToken: String)
  extends UserContext

case class FakeUserContext(
                            serviceSource: ServiceSource
                          )
  extends UserContext

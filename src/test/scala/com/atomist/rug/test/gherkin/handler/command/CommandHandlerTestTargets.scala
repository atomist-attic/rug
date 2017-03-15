package com.atomist.rug.test.gherkin.handler.command

import com.atomist.source.StringFileArtifact

object CommandHandlerTestTargets {

  val Feature1 =
    """
      |Feature: Australian political history
      | This is a test
      | to demonstrate that the Gherkin DSL
      | is a good fit for Rug BDD testing
      |
      |Scenario: Australian politics, 1972-1991
      | Given a sleepy country
      | When a visionary leader enters
      | Then excitement ensues
    """.stripMargin

  val Feature1File = StringFileArtifact(
    ".atomist/tests/handlers/command/Feature1.feature",
    Feature1
  )

}

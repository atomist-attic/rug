package com.atomist.rug.test.gherkin.handler.event

import com.atomist.source.StringFileArtifact

object EventHandlerTestTargets {

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
    ".atomist/tests/handlers/event/Feature1.feature",
    Feature1
  )

  val Feature2 =
    """
      |Feature: World should return messages not just plans
      |
      |Scenario: Let me return a message for you
      | When i call you
      | Then you call me
      | Then to greet me
    """.stripMargin

  val Feature2File = StringFileArtifact(
    ".atomist/tests/handlers/event/Feature2.feature",
    Feature2
  )

}

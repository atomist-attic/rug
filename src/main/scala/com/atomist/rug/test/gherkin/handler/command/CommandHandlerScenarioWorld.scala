package com.atomist.rug.test.gherkin.handler.command

import com.atomist.project.archive.Rugs
import com.atomist.rug.runtime.CommandHandler
import com.atomist.rug.test.gherkin.{Definitions, GherkinExecutionListener}
import com.atomist.rug.test.gherkin.handler.AbstractHandlerScenarioWorld
import com.atomist.tree.IdentityTreeMaterializer

/**
  * Scenario world for use to test command handlers.
  * Allows us to invoke a registered command handler explicitly
  */
class CommandHandlerScenarioWorld(definitions: Definitions, rugs: Option[Rugs] = None, listeners: Seq[GherkinExecutionListener])
  extends AbstractHandlerScenarioWorld(definitions, rugs, listeners) {

  def invokeHandler(handler: CommandHandler, params: Any): Unit = {
    val maybePlan = handler.handle(createRugContext(IdentityTreeMaterializer), parameters(params))
    maybePlan.foreach(plan => recordPlan(handler.name, plan))
  }

}

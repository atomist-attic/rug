package com.atomist.rug.test.gherkin.handler.command

import com.atomist.project.archive.Rugs
import com.atomist.rug.runtime.CommandHandler
import com.atomist.rug.test.gherkin.Definitions
import com.atomist.rug.test.gherkin.handler.AbstractHandlerScenarioWorld
import com.atomist.tree.IdentityTreeMaterializer

class CommandHandlerScenarioWorld(definitions: Definitions, rugs: Option[Rugs] = None)
  extends AbstractHandlerScenarioWorld(definitions, rugs) {

  def invokeHandler(handler: CommandHandler, params: Any): Unit = {
    val plan = handler.handle(createRugContext(IdentityTreeMaterializer), parameters(params))
    plan.map(recordPlan)
  }

}

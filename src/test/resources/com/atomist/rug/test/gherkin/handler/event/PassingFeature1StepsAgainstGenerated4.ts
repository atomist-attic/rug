import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"

import * as cortex from "@atomist/rug/cortex/stub/Types"
 
Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   world.registerHandler("ReturnsEmptyPlanEventHandlerGen1")
   world.sendEvent(
       new cortex.Build()
        .withStatus("passed")
        .withRepo(
            new cortex.Repo().withOwner("atomist")
        )
   )
})
Then("excitement ensues", world => {
    return world.plan().messages.length == 0
})
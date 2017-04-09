import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"

import * as cortex from "@atomist/rug/cortex/stub/Types"

Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   world.registerHandler("ReturnsEmptyPlanEventHandlerGen3")
   world.sendEvent(
       new cortex.Build()
        .withOn(new cortex.Repo())
   )
})
Then("excitement ensues", world => {
    return world.plan().messages.length == 0
})
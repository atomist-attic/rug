import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"

import * as cortex from "@atomist/rug/cortex/stub/Types"
 
Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   world.registerHandler("ReturnsEmptyPlanEventHandlerGen2")
   world.sendEvent(new cortex.Build())
})
Then("excitement ensues", world => {
    return world.plan().messages.length == 0
})
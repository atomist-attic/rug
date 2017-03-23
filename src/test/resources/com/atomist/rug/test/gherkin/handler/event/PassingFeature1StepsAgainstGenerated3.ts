import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"

import {Build} from "@atomist/rug/ext_model_stub/Build"
import {Repo} from "@atomist/rug/ext_model_stub/Repo"
 
Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   world.registerHandler("ReturnsEmptyPlanEventHandlerGen3")
   world.sendEvent(
       new Build()
        .withOn(new Repo())
   )
})
Then("excitement ensues", world => {
    return world.plan().messages.length == 0
})
import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"

import {Build} from "@atomist/rug/cortex/stub/Build"
import {Repo} from "@atomist/rug/cortex/stub/Repo"
 
Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   world.registerHandler("ReturnsEmptyPlanEventHandlerGen1")
   world.sendEvent(
       new Build()
        .withStatus("passed")
        .withRepo(
            new Repo().withOwner("atomist")
        )
   )
})
Then("excitement ensues", world => {
    return world.plan().messages.length == 0
})
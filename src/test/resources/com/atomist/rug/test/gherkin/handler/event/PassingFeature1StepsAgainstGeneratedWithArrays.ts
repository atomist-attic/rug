import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"

import * as cortex from "@atomist/rug/cortex/stub/Types"

 
Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   world.registerHandler("ReturnsEmptyPlanEventHandlerGenWithArrays")
   let p = new cortex.Push().addCommits(new cortex.Commit())
   //console.log(`Contains are ${p.contains().length} `);
   world.sendEvent(p)
})
Then("excitement ensues", world => {
    return world.plan().messages.length == 0
})
import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"

import {Push} from "@atomist/rug/cortex/stub/Push"
import {Commit} from "@atomist/rug/cortex/stub/Commit"

 
Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   world.registerHandler("ReturnsEmptyPlanEventHandlerGenWithArrays")
   let p = new Push().addCommit(new Commit())
   //console.log(`Contains are ${p.contains().length} `);
   world.sendEvent(p)
})
Then("excitement ensues", world => {
    return world.plan().messages.length == 0
})
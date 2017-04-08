import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"

import {Build} from "@atomist/rug/cortex/stub/Types"
 
Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   world.registerHandler("ReturnsEmptyPlanEventHandlerGen1")
   let b = new Build()
   world.sendEvent(b)
   // Check we don't break with toString problems
   let stringIt = String(b)
   let stringItAgain = "" + b
   let andThis = "" + world
})
Then("excitement ensues", world => {
    "" + world.plan()
    "" + world.plan().messages
    return world.plan().messages.length == 0
})
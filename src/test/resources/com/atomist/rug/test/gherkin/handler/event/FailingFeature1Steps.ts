import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"
import * as node from "../../../handlers/event/Nodes"

Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
    // Will fail as we don't register a handler
   //world.registerHandler("ReturnsEmptyPlanEventHandler")
   world.sendEvent(new node.Commit)
})
Then("excitement ensues", world => {
    return world.plan().messages.length == 0
})
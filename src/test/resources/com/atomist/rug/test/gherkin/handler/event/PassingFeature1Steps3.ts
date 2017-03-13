import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"
import * as node from "../../../handlers/event/Nodes"

Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   world.registerHandler("ReturnsEmptyPlanEventHandler2")
   world.sendEvent(new node.Commit)
})
Then("excitement ensues", (p,world) => {
    return !world.plan()
})
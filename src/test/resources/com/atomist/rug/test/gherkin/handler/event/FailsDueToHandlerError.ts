import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"
import * as node from "../../../handlers/event/Nodes"

Given("a sleepy country", world => {
    world.registerHandler("AngryHandler")
})
When("a visionary leader enters", world => {
   world.sendEvent(new node.Commit().withSha("666"))
})
Then("excitement ensues", world => {

})
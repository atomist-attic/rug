import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"
import * as node from "../../../handlers/event/Nodes"

Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   world.registerHandler("ReturnsAMessage")
   let c = new node.Commit().withMadeBy(new node.Person("Ebony"))
   world.sendEvent(c)
})
Then("excitement ensues", world => {
    // Void return is the same as returning true.
    // This enables us to use assertion frameworks such as Chai
})
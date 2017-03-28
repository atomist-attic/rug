import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"
import * as node from "../../../handlers/command/Nodes"

Given("a sleepy country", f => {
    //console.log("Given invoked for handler")
})
When("a visionary leader enters", world => {
   let handler = world.commandHandler("GoesOffGraph")
   let c = new node.Commit().withMadeBy(new node.Person("Ebony"));
   world.addToRootContext(c);
   world.invokeHandler(handler, {})
})
Then("excitement ensues", world => {
    return world.plan().messages.length == 0
})
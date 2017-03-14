import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"

Given("a sleepy country", f => {
    //console.log("Given invoked for handler")
})
When("a visionary leader enters", world => {
   let handler = world.commandHandler("ReturnsOneMessageCommandHandler")
   world.invokeHandler(handler, {})
})
Then("excitement ensues", world => {
   console.log("The plan message were " + world.plan().messages())
    return world.plan().messages().length == 1
})
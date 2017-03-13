import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"

Given("a sleepy country", f => {
    //console.log("Given invoked for handler")
})
When("a visionary leader enters", (rugContext, world) => {
   let handler = world.commandHandler("ReturnsEmptyPlanCommandHandler")
   world.invokeHandler(handler, {})
})
Then("excitement ensues", (p,world) => {
    return world.plan().messages().length == 0
})
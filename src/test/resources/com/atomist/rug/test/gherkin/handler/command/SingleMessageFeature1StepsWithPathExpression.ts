import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"

Given("a sleepy country", f => {
})
When("a visionary leader enters", (rugContext, world) => {
   let handler = world.commandHandler("RunsPathExpressionCommandHandler")
   world.invokeHandler(handler, {})
})
Then("excitement ensues", (p,world) => {
   //console.log("The plan message were " + world.plan().messages())
    return world.plan().messages().length == 1
})
import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"

Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   let handler = world.commandHandler("RunsPathExpressionCommandHandler")
   world.invokeHandler(handler, {})
})
Then("excitement ensues", world => {
    return world.plan().messages().length == 1
})
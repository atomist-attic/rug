import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"
import * as node from "../../../handlers/event/Nodes"

When("i call you", world => {
   world.registerHandler("ReturnsAMessage")
   world.registerHandler("AngryHandler")
   world.sendEvent(new node.Commit)
   world.sendEvent(new node.Commit)
   world.sendEvent(new node.GitHubId("myId"))
})
Then("you call me", world => {
    return world.plan().messages[0] != null
})
Then("to greet me", world => {
    let message = world.plan().messages[0]
    return (message.body === "Hello there!")
})
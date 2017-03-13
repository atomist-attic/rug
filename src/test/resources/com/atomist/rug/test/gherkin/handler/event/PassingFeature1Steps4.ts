import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"
import * as node from "../../../handlers/event/Nodes"

Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   world.registerHandler("ReturnsEmptyPlanEventHandler3")
   let c = new node.Commit().withMadeBy(
       new node.Person("Ebony").withGitHubId(new node.GitHubId("gogirl"))
    )
   world.sendEvent(c)
})
Then("excitement ensues", (p,world) => {
    return world.plan() != null
})
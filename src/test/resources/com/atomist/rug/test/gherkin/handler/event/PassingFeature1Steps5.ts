import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"
import * as node from "../../../handlers/event/Nodes"

Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   world.registerHandler("ReturnsEmptyPlanEventHandler3")
   // We can survive this
   world.registerHandler("AngryHandler")

   let c = new node.Commit().withMadeBy(
       new node.Person("Ebony").withGitHubId(new node.GitHubId("gogirl"))
    )
   world.sendEvent(c)
})
Then("excitement ensues", world => {
    if (world.plans().length != 1) throw new Error(`Needed 1 plan, not ${world.plans().length}`)
       world.requiredPlan()
    if (world.plan() == null) throw new Error("Plan was null")
    if (world.plan().messages.length != 0) throw new Error("Too many messages")
    if (world.plans()[0].messages.length != 0) throw new Error("Too many messages when going through array")
})
import {Given,When,Then, HandlerScenarioWorld} from "@atomist/rug/test/handler/Core"
import * as node from "../../../handlers/event/Nodes"

let goodHandler = "ReturnsEmptyPlanEventHandler3"

let badHandler = "AngryHandler"

Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   world.registerHandler(goodHandler)
   // We can survive this
   world.registerHandler(badHandler)

   let c = new node.Commit().withMadeBy(
       new node.Person("Ebony").withGitHubId(new node.GitHubId("gogirl"))
    )
   world.sendEvent(c)
})
Then("excitement ensues", world => {
    if (world.planCount() != 1) throw new Error(`Needed 1 plan, not ${world.planCount()}`)
    world.requiredPlan()
    if (world.plan() == null) throw new Error("Plan was null")
    if (world.plan().messages.length != 0) throw new Error("Too many messages")
    if (world.planFor(goodHandler).messages.length != 0) throw new Error("Too many messages when going through array")
    if (world.planFor(badHandler) != null) throw new Error("Bad handler should have returned null plan")
})
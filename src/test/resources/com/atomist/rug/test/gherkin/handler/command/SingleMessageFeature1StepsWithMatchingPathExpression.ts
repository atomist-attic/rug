import { Given, When, Then, HandlerScenarioWorld } from "@atomist/rug/test/handler/Core"
import * as node from "../../../handlers/command/Nodes"

Given("a sleepy country", f => {
});
When("a visionary leader enters", (rugContext, world) => {
    let handler = world.commandHandler("RunsMatchingPathExpressionCommandHandler");
    let c = new node.Commit().withMadeBy(new node.Person("Ebony"));
    world.addToRootContext(c);
    world.invokeHandler(handler, {});
});
Then("excitement ensues", world => {
    // console.log("The plan messages were " + world.plan().messages().length);
    // TODO we are seeing 2. This is wrong
    return world.plan().messages().length == 1;
});

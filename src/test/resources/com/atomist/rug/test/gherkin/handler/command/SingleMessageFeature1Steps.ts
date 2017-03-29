import { Given, When, Then, HandlerScenarioWorld } from "@atomist/rug/test/handler/Core"

Given("a sleepy country", f => {
    // console.log("Given invoked for handler");
});
When("a visionary leader enters", world => {
    let handler = world.commandHandler("ReturnsOneMessageCommandHandler");
    world.invokeHandler(handler, {});
});
Then("excitement ensues", world => {
    //console.log(`World plan is ${world.plan()}, first message body is ${world.plan().messages[0].body.value}`)
    let drillDown = world.plan().messages[0].body;
    if (drillDown != "woot")
        throw new Error(`Unexpected drill-down value [${drillDown}]`)
    return world.plan().messages.length == 1;
});

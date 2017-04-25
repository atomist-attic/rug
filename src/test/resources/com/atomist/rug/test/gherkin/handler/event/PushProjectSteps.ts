import { Given, When, Then, EventHandlerScenarioWorld } from "@atomist/rug/test/handler/Core"
import { rugAssertEqual as assertEqual } from "@atomist/rug/test/handler/Core"

import * as cortex from "@atomist/rug/cortex/stub/Types"

const projectName = "p1";

When("a push occurs", (world: EventHandlerScenarioWorld) => {
   world.registerHandler("GithubPushAS")
   const owner = "atomist";
   const repoName = "rug";
   const p = world.emptyProject(projectName);
   p.addFile("pom.xml", "<xml></xml>")
   let repo = new cortex.Repo().withOwner(owner).withName(repoName);
   world.defineRepo(owner, repoName, "master", p);
   world.addToRootContext(repo);

   const push = new cortex.Push().withRepo(
        new cortex.Repo().withOwner(owner).withName(repoName)
    );
    //console.log(`Publishing event ${JSON.stringify(push)}`);
    world.sendEvent(push);
})

Then("we can access project", world => {
    const mess = world.plan().messages[0].body;
    assertEqual(world.plan().messages.length, 1);
    if (mess.indexOf(projectName) == -1)
        throw new Error(`Don't like message [${mess}]`);
})
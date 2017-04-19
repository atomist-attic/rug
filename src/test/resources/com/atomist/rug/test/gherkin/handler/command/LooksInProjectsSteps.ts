import { Given, When, Then, HandlerScenarioWorld } from "@atomist/rug/test/handler/Core"
import { rugAssertEqual as assertEqual } from "@atomist/rug/test/handler/Core"

import * as cortex from "@atomist/rug/cortex/stub/Types"

Given("a sleepy country", f => {
})
When("a visionary leader enters", world => {
   const handler = world.commandHandler("LooksInProjects")
   const owner = "atomist";
   const repoName = "rug";
   const p = world.emptyProject("p1");
   p.addFile("pom.xml", "<xml></xml>")
   let repo = new cortex.Repo().withOwner(owner).withName(repoName);
   world.defineRepo(owner, repoName, "master", p);
   world.addToRootContext(repo);

   const p2 = world.emptyProject("p2");
   p2.addFile("pom.xml", "<xml></xml>")
   let repo2 = new cortex.Repo().withOwner("owner2").withName(repoName);
   world.defineRepo("owner2", repoName, "master", p2);
   world.addToRootContext(repo2);
   world.invokeHandler(handler, {});
})
Then("excitement ensues", world => {
    assertEqual(world.plan().messages.length, 2);
})
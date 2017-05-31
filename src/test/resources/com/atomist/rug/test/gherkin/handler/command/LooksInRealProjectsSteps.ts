
import { CloneInfo } from "@atomist/rug/test/ScenarioWorld"

import { Given, When, Then, HandlerScenarioWorld } from "@atomist/rug/test/handler/Core"
import { rugAssertEqual as assertEqual } from "@atomist/rug/test/handler/Core"

import * as cortex from "@atomist/rug/cortex/stub/Types"

Given("a sleepy country", f => {
});
When("a visionary leader enters", world => {
   const handler = world.commandHandler("LooksInProjects")
   const owner = "atomist";
   const repoName1 = "rugs";
   const p = world.emptyProject("p1");
   p.addFile("pom.xml", "<xml></xml>")
   let repo = new cortex.Repo().withOwner(owner).withName(repoName1);
   world.defineRepo(owner, repoName1, "master", p);

   const repoName2 = "rug";
   const repoId = new CloneInfo(owner, repoName2);
   const p2 = world.cloneRepo(repoId);
   let repo2 = new cortex.Repo().withOwner(owner).withName(repoName2);
   world.defineRepo(repoId, p2);

   const ct = new cortex.ChatTeam().addOrgs(new cortex.Org().addRepo(repo).addRepo(repo2));
   world.setRootContext(ct);

   world.invokeHandler(handler, {});
});
Then("excitement ensues", world => {
    assertEqual(world.plan().messages.length, 2);
});
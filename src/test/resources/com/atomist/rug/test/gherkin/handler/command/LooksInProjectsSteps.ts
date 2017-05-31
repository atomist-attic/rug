import { Given, When, Then, CommandHandlerScenarioWorld, HandlerScenarioWorld } from "@atomist/rug/test/handler/Core"
import { rugAssertEqual as assertEqual } from "@atomist/rug/test/handler/Core"

import * as cortex from "@atomist/rug/cortex/stub/Types"

Given("a sleepy country", f => {
});
When("a visionary leader enters", (world: CommandHandlerScenarioWorld) => {
   const handler = world.commandHandler("LooksInProjects");
   const owner = "atomist";
   const repoName = "rug";
   const p = world.emptyProject("p1");

   const pc = world.projectStartingWith(p);
   if (pc.name != p.name) throw "Project names differ";
   if (pc.totalFileCount != p.totalFileCount) throw "Project file counts differ";

   p.addFile("pom.xml", "<xml></xml>");
   let repo = new cortex.Repo().withOwner(owner).withName(repoName);
   world.defineRepo(owner, repoName, "master", p);

   const p2 = world.emptyProject("p2");
   p2.addFile("pom.xml", "<xml></xml>");
   let repo2 = new cortex.Repo().withOwner(owner).withName(repoName + "2");
   world.defineRepo(repo2.owner, repo2.name, "master", p2);
   const ct = new cortex.ChatTeam().addOrgs(new cortex.Org().addRepo(repo).addRepo(repo2));
   world.setRootContext(ct);
   world.invokeHandler(handler, {});
});
Then("excitement ensues", world => {
    assertEqual(world.plan().messages.length, 2);
});
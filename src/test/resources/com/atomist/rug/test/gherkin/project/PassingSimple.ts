import { Project } from "@atomist/rug/model/Core"
import { ProjectEditor } from "@atomist/rug/operations/ProjectEditor"
import { Given, When, Then } from "@atomist/rug/test/project/Core"

Given("a visionary leader", p => {
    p.addFile("Gough", "Maintain the rage");
});
When("politics takes its course", (p, world) => {
    // console.log(`The world is $${world}`);
});
Then("changes were made", p => true); // Override this one for this test
Then("one edit was made", p => true);
Then("the rage is maintained", p => p.fileExists("Gough"));
Then("the rage has a name", p => p.name != null && p.name != "" && p.name.length > 0);
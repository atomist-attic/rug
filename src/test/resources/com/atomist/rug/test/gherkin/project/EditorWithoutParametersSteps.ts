import { Project } from "@atomist/rug/model/Core"
import { Given, When, Then, ProjectScenarioWorld } from "@atomist/rug/test/project/Core"

import { AlpEditor } from "../../editors/AlpEditors"

Given("a visionary leader", p => {
    p.addFile("Gough", "Maintain the rage");
})
When("politics takes its course", (p, w) => {
    let world = w as ProjectScenarioWorld;
    world.editWith(world.editor("AlpEditor"));
});
Then("one edit was made", (p, world) => {
    return world.editorsRun() == 1;
});
Then("the rage is maintained", p => {
    return p.fileExists("Paul");
});
Then("the rage has a name", p => {
    let name = p.name;
    return name != null && name != "" && name.length > 0;
});

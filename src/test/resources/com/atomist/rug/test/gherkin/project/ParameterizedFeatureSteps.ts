import { Project } from "@atomist/rug/model/Core"
import { ProjectEditor } from "@atomist/rug/operations/ProjectEditor"
import { Given, When, Then, ProjectScenarioWorld } from "@atomist/rug/test/project/Core"

//import { AlpEditor } from "../../editors/AlpEditors"

Given("a file named ([a-zA-Z]+)", (p, world, filename) => {
    p.addFile(filename, "Maintain the rage");
}) 
When("do nothing", p => {
});
Then("the project has (\\d+) files", (p, world, filecount) => {
    return p.totalFileCount == filecount;
});
Then("the project has a file named ([a-zA-Z]+)", (p, world, filename) => {
    //console.log(`First arg = ${filename}`)
    return p.fileExists(filename)
});

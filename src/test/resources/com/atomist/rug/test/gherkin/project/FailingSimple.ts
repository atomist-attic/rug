import { Project } from "@atomist/rug/model/Core"
import { Given, When, Then } from "@atomist/rug/test/project/Core"

Given("a visionary leader", p => {
    p.addFile("Gough", "Maintain the rage");
})
When("politics takes its course", p => {
    p.addFile("Malcolm", "Life wasn't meant to be easy");
    p.deleteFile("Gough");
})
Then("the rage is maintained", p => p.fileExists("Gough"));

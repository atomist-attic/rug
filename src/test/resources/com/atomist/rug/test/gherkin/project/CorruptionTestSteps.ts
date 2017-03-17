import { Project } from "@atomist/rug/model/Core"
import { ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
import { Given, When, Then } from "@atomist/rug/test/project/Core"
import * as helpers from "@atomist/rug/test/project/Helpers"

import {FindCorruption} from "../../editors/FindCorruption"

Given("a number of files", p => {
    p.addFile("NSW", "Wran");
    p.addFile("Victoria", "Cain");
    p.addFile("WA", "Brian Burke and WA Inc");
});
When("run corruption reviewer", (p, world) => {
    let r = new FindCorruption();
    let rr = r.review(p);
    world.put("review", rr);
});
Then("we have comments", (p, world) => {
    if (helpers.prettyListFiles(p).indexOf("NSW") == -1) throw new Error("Bad pretty list");
    if (helpers.dump(p, "NSW").indexOf("Wran") == -1)  throw new Error("Bad dump");
    let rr = world.get("review");
    return rr.comments.length == 1;
});
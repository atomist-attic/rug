import { EventHandler, Tags } from "@atomist/rug/operations/Decorators";
import { HandleEvent, DirectedMessage, EventPlan, ChannelAddress } from "@atomist/rug/operations/Handlers";
import { GraphNode, Match, PathExpression } from "@atomist/rug/tree/PathExpression";
import { Project } from "@atomist/rug/model/Project";

import { Push } from "@atomist/rug/cortex/Push";

@EventHandler("GithubPushAS", "Handle push events into artifact source",
    new PathExpression<Push, Project>(
        `/Push()
            /repo::Repo()
              /master::Project()`))
@Tags("github", "push")
class NewPushAS implements HandleEvent<Push, Project> {
    public handle(event: Match<Push, Project>): EventPlan {
        //console.log(`Handler matched ${event.root}`);

        const project: Project = event.matches[0];
        const messageBody = `Found project with name ${project.name} with ${project.fileCount} files`;
        const channel = new ChannelAddress("#flood");
        const message = new DirectedMessage(messageBody, channel, "text/plain");

        const plan = new EventPlan();
        plan.add(message);
        return plan;
    }
}
export const push = new NewPushAS();
import { HandleEvent, Plan, Message } from '@atomist/rug/operations/Handlers'
import { GraphNode, Match, PathExpression } from '@atomist/rug/tree/PathExpression'
import { EventHandler, Tags } from '@atomist/rug/operations/Decorators'

import * as node from "./Nodes"

@EventHandler("ReturnsEmptyPlanEventHandler", "Handles a new Commit event",
    new PathExpression<GraphNode, GraphNode>("/Commit"))
@Tags("github", "build")
class ReturnsEmptyPlanEventHandler implements HandleEvent<GraphNode, GraphNode> {

    handle(event: Match<GraphNode, GraphNode>) {
        return new Plan();
    }
}
export const handler = new ReturnsEmptyPlanEventHandler()

@EventHandler("ReturnsEmptyPlanEventHandler2", "Handles a new Commit event",
    new PathExpression<GraphNode, GraphNode>("/Commit/madeBy[@name='Ebony']"))
@Tags("github", "issue")
class ReturnsEmptyPlanEventHandler2 implements HandleEvent<GraphNode, GraphNode> {

    handle(event: Match<GraphNode, GraphNode>) {
        return new Plan();
    }
}
export const handler2 = new ReturnsEmptyPlanEventHandler2();


@EventHandler("ReturnsEmptyPlanEventHandler2a", "Handles a new Commit event",
    new PathExpression<node.Commit, node.Person>("/Commit/Person()[@name='Ebony']"))
@Tags("github", "issue")
class ReturnsEmptyPlanEventHandler2a implements HandleEvent<GraphNode, GraphNode> {

    handle(m: Match<node.Commit, node.Person>) {
        let peep = m.matches()[0];
        // console.log(`Match ${m}: peep=${peep}`);
        return new Plan();
    }
}
export const handler2a = new ReturnsEmptyPlanEventHandler2a();


@EventHandler("ReturnsEmptyPlanEventHandler3", "Handles a new Commit event",
    new PathExpression<node.Commit, node.Person>(
        `/Commit/Person()[@name='Ebony']/gitHubId
        `))
@Tags("github", "issue")
class ReturnsEmptyPlanEventHandler3 implements HandleEvent<node.Commit, node.GitHubId> {

    handle(m: Match<node.Commit, node.GitHubId>) {
        let ghid: node.GitHubId = m.matches()[0]
        if (ghid.id() != "gogirl") throw new Error(`Unexpected github id ${ghid.id()}`)
        if (ghid.address() != "/madeBy/gitHubId") throw new Error(`Unexpected address [${ghid.address()}]`)

        return new Plan();
    }
}
export const handler3 = new ReturnsEmptyPlanEventHandler3();

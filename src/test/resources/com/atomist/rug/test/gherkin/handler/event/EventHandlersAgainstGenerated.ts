import { HandleEvent, EventPlan } from '@atomist/rug/operations/Handlers'
import { GraphNode, Match, PathExpression } from '@atomist/rug/tree/PathExpression'

import { EventHandler, Tags } from '@atomist/rug/operations/Decorators'

import {Build} from "@atomist/rug/cortex/Build"
import {Push} from "@atomist/rug/cortex/Push"

/**
 * Works against generated model
 */
@EventHandler("ReturnsEmptyPlanEventHandlerGen1", "Handles a new Commit event",
    new PathExpression<GraphNode, Build>(
        `/Build()`))
@Tags("build")
class ReturnsEmptyPlanEventHandlerGen1 implements HandleEvent<GraphNode, Build> {

    handle(m: Match<GraphNode, Build>) {
        let b: Build = m.matches[0]
        b.status + ""   // Evaluate this stringification doesn't break
        return new EventPlan();
    }
}
export const handler1 = new ReturnsEmptyPlanEventHandlerGen1();

@EventHandler("ReturnsEmptyPlanEventHandlerGen1WithPathExpression", "Handles a new Commit event",
    new PathExpression<GraphNode, Build>(
        `/Build()`))
@Tags("build")
class ReturnsEmptyPlanEventHandlerGen1WithPathExpression implements HandleEvent<GraphNode, Build> {

    handle(m: Match<GraphNode, Build>) {
        let b: Build = m.matches[0];
        m.pathExpressionEngine.evaluate(b, "/anythingAtAll");
        if (!m.contextRoot) throw new Error("No context root");
        let s = m.pathExpressionEngine.evaluate(m.contextRoot, "/anythingAtAll");
        return new EventPlan();
    }
}
export const handler1a = new ReturnsEmptyPlanEventHandlerGen1WithPathExpression();

@EventHandler("ReturnsEmptyPlanEventHandlerGenWithArrays", "Handles a new Commit event",
    new PathExpression<GraphNode, Push>(
        `/Push()`))
@Tags("push")
class ReturnsEmptyPlanEventHandlerGenWithArrays implements HandleEvent<GraphNode, Push> {

    handle(m: Match<GraphNode, Push>) {
        let p: Push = m.matches[0]

        if (p.commits.length != 1) throw new Error("No commits")

        return new EventPlan();
    }
}
export const handler2 = new ReturnsEmptyPlanEventHandlerGenWithArrays();

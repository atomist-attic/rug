import { HandleEvent, Plan, Message } from '@atomist/rug/operations/Handlers'
import { GraphNode, Match, PathExpression } from '@atomist/rug/tree/PathExpression'

import { EventHandler, Tags } from '@atomist/rug/operations/Decorators'

import {Build} from "@atomist/rug/cortex/Build"
import {Push} from "@atomist/rug/cortex/Push"

import * as buildstub from "@atomist/rug/cortex/stub/Build"
import * as repostub from "@atomist/rug/cortex/stub/Repo"
import * as ccstub from "@atomist/rug/cortex/stub/ChatChannel"


/**
 * Works against generated model
 */
@EventHandler("ReturnsEmptyPlanEventHandlerGen1", "Handles a new Commit event",
    new PathExpression<GraphNode, Build>(
        `/Build()`))
@Tags("build")
class ReturnsEmptyPlanEventHandlerGen1 implements HandleEvent<GraphNode, Build> {

    handle(m: Match<GraphNode, Build>) {
        let b: Build = m.matches()[0]
        //if (ghid.id() != "gogirl") throw new Error(`Unexpected github id ${ghid.id()}`)
        //if (ghid.address() != "/madeBy/gitHubId") throw new Error(`Unexpected address [${ghid.address()}]`)

        return new Plan();
    }
}
export const handler1 = new ReturnsEmptyPlanEventHandlerGen1();

@EventHandler("ReturnsEmptyPlanEventHandlerGenWithArrays", "Handles a new Commit event",
    new PathExpression<GraphNode, Push>(
        `/Push()`))
@Tags("push")
class ReturnsEmptyPlanEventHandlerGenWithArrays implements HandleEvent<GraphNode, Push> {

    handle(m: Match<GraphNode, Push>) {
        let p: Push = m.matches()[0]

        if (p.commits.length != 1) throw new Error("No commits")

        return new Plan();
    }
}
export const handler2 = new ReturnsEmptyPlanEventHandlerGenWithArrays();

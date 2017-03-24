import { HandleEvent, Plan, Message } from '@atomist/rug/operations/Handlers'
import { GraphNode, Match, PathExpression } from '@atomist/rug/tree/PathExpression'
import * as query from '@atomist/rug/tree/QueryByExample'

import { EventHandler, Tags } from '@atomist/rug/operations/Decorators'

import {Build} from "@atomist/rug/ext_model/Build"
import * as buildstub from "@atomist/rug/ext_model_stub/Build"
import * as repostub from "@atomist/rug/ext_model_stub/Repo"
import * as ccstub from "@atomist/rug/ext_model_stub/ChatChannel"


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

@EventHandler("ReturnsEmptyPlanEventHandlerGen2", "Handles a new Commit event",
    query.forRoot<Build>(
        new buildstub.Build()
    ))
@Tags("build")
class ReturnsEmptyPlanEventHandlerGen2 implements HandleEvent<GraphNode, Build> {

    handle(m: Match<GraphNode, Build>) {
        let b: Build = m.matches()[0]
        //if (ghid.id() != "gogirl") throw new Error(`Unexpected github id ${ghid.id()}`)
        //if (ghid.address() != "/madeBy/gitHubId") throw new Error(`Unexpected address [${ghid.address()}]`)

        return new Plan();
    }
}
export const handler2 = new ReturnsEmptyPlanEventHandlerGen2();


@EventHandler("ReturnsEmptyPlanEventHandlerGen3", "Handles a new Commit event",
    query.forRoot<Build>(
        new buildstub.Build()
            .withOn(new repostub.Repo()
        )
    ))
@Tags("build")
class ReturnsEmptyPlanEventHandlerGen3 implements HandleEvent<GraphNode, Build> {

    handle(m: Match<GraphNode, Build>) {
        let b: Build = m.matches()[0]
        //console.log(`The build is ${b} with repo ${b.on()}`)
        //if (ghid.id() != "gogirl") throw new Error(`Unexpected github id ${ghid.id()}`)
        //if (ghid.address() != "/madeBy/gitHubId") throw new Error(`Unexpected address [${ghid.address()}]`)

        return new Plan();
    }
}
export const handler3 = new ReturnsEmptyPlanEventHandlerGen3();


@EventHandler("ReturnsEmptyPlanEventHandlerGen4", "Handles a new Commit event",
    query.forRoot<Build>(
        new buildstub.Build()
            .withOn(new repostub.Repo()
                .withOwner("atomist"))
        )
)
@Tags("build")
class ReturnsEmptyPlanEventHandlerGen4 implements HandleEvent<GraphNode, Build> {

    handle(m: Match<GraphNode, Build>) {
        let b: Build = m.matches()[0]
        //console.log(`The build is ${b} with repo ${b.on()}`)
        //if (ghid.id() != "gogirl") throw new Error(`Unexpected github id ${ghid.id()}`)
        //if (ghid.address() != "/madeBy/gitHubId") throw new Error(`Unexpected address [${ghid.address()}]`)

        return new Plan();
    }
}
export const handler4 = new ReturnsEmptyPlanEventHandlerGen4();

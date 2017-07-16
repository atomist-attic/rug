import { HandleCommand, Instruction, Response, HandlerContext, CommandPlan, ResponseMessage } from '@atomist/rug/operations/Handlers'
import { CommandHandler, Secrets, Parameter, Tags, Intent } from '@atomist/rug/operations/Decorators'
import { Project } from "@atomist/rug/model/Project"

import * as node from "./Nodes"

@CommandHandler("ReturnsEmptyPlanCommandHandler", "Return empty plan")
@Tags("kitty", "youtube", "slack")
@Intent("show me kitties", "cats please")
@Secrets("atomist/user_token", "atomist/showmethemoney")
class ReturnsEmptyPlanCommandHandler implements HandleCommand {

    handle(ctx: HandlerContext): CommandPlan {
        let result = new CommandPlan();
        // result.add({instruction: {kind: "execute", name: "ExampleFunction", parameters: {thingy: "woot"}}});
        return result;
    }
}

export const command1 = new ReturnsEmptyPlanCommandHandler();


@CommandHandler("ReturnsOneMessageCommandHandler", "Returns one message")
@Tags("kitty", "youtube", "slack")
@Secrets("atomist/user_token", "atomist/showmethemoney")
class ReturnsOneMessageCommandHandler implements HandleCommand {

    handle(ctx: HandlerContext): CommandPlan {
        let result = new CommandPlan()
        if (ctx.teamId == null)
            throw new Error("Cannot get at team id")

        result.add(new ResponseMessage("woot"))
        // console.log(`The constructed plan messages were ${result.messages()},size=${result.messages().length}`)
        return result;
    }
}
export const command2 = new ReturnsOneMessageCommandHandler();


@CommandHandler("RunsPathExpressionCommandHandler", "Returns one message")
@Tags("path_expression")
class RunsPathExpressionCommandHandler implements HandleCommand {

    handle(ctx: HandlerContext): CommandPlan {
        let result = new CommandPlan()
        let eng = ctx.pathExpressionEngine

        const findPerson = "/Commit/Person()[@name='Ebony']"

        eng.with<node.Person>(ctx.contextRoot, findPerson, peep => {
            throw new Error(`Shouldn't have found a person but found ${peep}`)
        })
        result.add(new ResponseMessage("woot"))
        // console.log(`The constructed plan messages were ${result.messages()},size=${result.messages().length}`)
        return result;
    }
}

export const command3 = new RunsPathExpressionCommandHandler();


@CommandHandler("RunsMatchingPathExpressionCommandHandler", "Returns one message")
@Tags("path_expression")
class RunsMatchingPathExpressionCommandHandler implements HandleCommand {

    handle(ctx: HandlerContext): CommandPlan {
        let result = new CommandPlan()
        const eng = ctx.pathExpressionEngine

        const findPerson = "/Commit/Person()[@name='Ebony']"
        eng.with<node.Person>(ctx.contextRoot, findPerson, peep => {
            // console.log(`Adding message with person name=${peep.name},obj=${peep}`)
            result.add(
                new ResponseMessage(peep.name))
        })
        // console.log(`The constructed plan messages were ${result.messages()},size=${result.messages().length}`)
        return result;
    }
}
export const command4 = new RunsMatchingPathExpressionCommandHandler();

@CommandHandler("GoesOffGraph", "Goes off graph")
@Tags("path_expression")
class GoesOffGraph implements HandleCommand {

    handle(ctx: HandlerContext): CommandPlan {
        let result = new CommandPlan()
        const eng = ctx.pathExpressionEngine

        const findPerson = "/Commit/Person()[@name='Ebony']"
        eng.with<node.Person>(ctx.contextRoot, findPerson, peep => {
            // Deliberate error should throw exception
            console.log(JSON.stringify(peep))
            peep.gitHubId.id
            // console.log(`Adding message with person name=${peep.name},obj=${peep}`)
            result.add(new ResponseMessage(peep.name))
        })
        // console.log(`The constructed plan messages were ${result.messages()},size=${result.messages().length}`)
        return result;
    }
}
export const command5 = new GoesOffGraph();


@CommandHandler("LooksInProjects", "Looks in projects")
@Tags("path_expression")
class LooksInProjects implements HandleCommand {

    handle(ctx: HandlerContext): CommandPlan {
        let result = new CommandPlan();
        const eng = ctx.pathExpressionEngine;

        // Find all Maven projects
        const mavenProjects = "/orgs::Org()/repo::Repo()/master::Project()[/pom.xml]";
        eng.with<Project>(ctx.contextRoot, mavenProjects, p => {
            result.add(new ResponseMessage(`${p.name} is a Maven project`))
        });
        return result;
    }
}
export const command6 = new LooksInProjects();

import {HandleCommand, Instruction, Response, HandlerContext, Plan, Message} from '@atomist/rug/operations/Handlers'
import {CommandHandler, Secrets, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

import * as node from "./Nodes"

@CommandHandler("ReturnsEmptyPlanCommandHandler","Return empty plan")
@Tags("kitty", "youtube", "slack")
@Intent("show me kitties","cats please")
@Secrets("atomist/user_token", "atomist/showmethemoney")
class ReturnsEmptyPlanCommandHandler implements HandleCommand {

  handle(ctx: HandlerContext) : Plan {

    let result = new Plan()
    //result.add({instruction: {kind: "execute", name: "ExampleFunction", parameters: {thingy: "woot"}}})
    return result;
  }
}

export let command1 = new ReturnsEmptyPlanCommandHandler();


@CommandHandler("ReturnsOneMessageCommandHandler","Returns one message")
@Tags("kitty", "youtube", "slack")
@Secrets("atomist/user_token", "atomist/showmethemoney")
class ReturnsOneMessageCommandHandler implements HandleCommand {

  handle(ctx: HandlerContext) : Plan {
    let result = new Plan()
    if (ctx.teamId() == null)
      throw new Error("Cannot get at team id")

    result.add(new Message("woot").withCorrelationId("dude"))
    console.log(`The constructed plan messages were ${result.messages()},size=${result.messages().length}`)
    return result;
  }
}
export let command2 = new ReturnsOneMessageCommandHandler();


@CommandHandler("RunsPathExpressionCommandHandler","Returns one message")
@Tags("path_expression")
class RunsPathExpressionCommandHandler implements HandleCommand {

  handle(ctx: HandlerContext) : Plan {
    let result = new Plan()
    const eng = ctx.pathExpressionEngine()

    const findPerson = "/Commit/Person()[@name='Ebony']"

    eng.with<node.Person>(ctx.contextRoot(), findPerson, peep => {
      throw new Error(`Shouldn't have found a person but found ${peep}`)
    })
    result.add(new Message("woot").withCorrelationId("dude"))
    console.log(`The constructed plan messages were ${result.messages()},size=${result.messages().length}`)
    return result;
  }
}

export let command3 = new RunsPathExpressionCommandHandler();
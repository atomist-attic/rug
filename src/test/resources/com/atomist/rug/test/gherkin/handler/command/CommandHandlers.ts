import {HandleCommand, Instruction, Response, HandlerContext, Plan, Message} from '@atomist/rug/operations/Handlers'
import {CommandHandler, Secrets, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("ReturnsEmptyPlanCommandHandler","Search Youtube for kitty videos and post results to slack")
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


@CommandHandler("ReturnsOneMessageCommandHandler","Search Youtube for kitty videos and post results to slack")
@Tags("kitty", "youtube", "slack")
@Intent("show me kitties","cats please")
@Secrets("atomist/user_token", "atomist/showmethemoney")
class ReturnsOneMessageCommandHandler implements HandleCommand {

  handle(ctx: HandlerContext) : Plan {
    let result = new Plan()
    result.add(new Message("woot").withCorrelationId("dude"))
    console.log(`The constructed plan messages were ${result.messages()},size=${result.messages().length}`)
    return result;
  }
}

export let command2 = new ReturnsOneMessageCommandHandler();
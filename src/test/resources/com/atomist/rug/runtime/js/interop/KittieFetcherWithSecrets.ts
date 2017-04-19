import {HandleCommand, Instruction, Response, HandlerContext, CommandPlan} from '@atomist/rug/operations/Handlers'
import {CommandHandler, Secrets, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
@Tags("kitty", "youtube", "slack")
@Intent("show me kitties","cats please")
@Secrets("atomist/user_token", "atomist/showmethemoney")
class KittieFetcher implements HandleCommand{

  handle(ctx: HandlerContext) : CommandPlan {

    let result = new CommandPlan()
    result.add({instruction: {kind: "execute", name: "ExampleFunction", parameters: {thingy: "woot"}}})
    return result;
  }
}

export let command = new KittieFetcher();

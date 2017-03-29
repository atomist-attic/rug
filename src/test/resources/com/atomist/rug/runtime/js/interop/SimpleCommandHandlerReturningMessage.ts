import {HandleCommand, Instruction, Response, HandlerContext, Plan, Message} from '@atomist/rug/operations/Handlers'
import {CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
class KittieFetcher implements HandleCommand{

  handle(ctx: HandlerContext) : Plan {
    return new Plan().add(new Message("Up and at 'em!"));
  }
}

export let command = new KittieFetcher();

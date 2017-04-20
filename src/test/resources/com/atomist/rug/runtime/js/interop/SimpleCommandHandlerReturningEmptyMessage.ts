import {HandleCommand, Instruction, Response, HandlerContext, CommandPlan, DirectedMessage, UserAddress} from '@atomist/rug/operations/Handlers'
import {CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("ShowMeTheKitties", "Search Youtube for kitty videos and post results to slack")
class KittieFetcher implements HandleCommand{

  handle(ctx: HandlerContext) {
    return new CommandPlan().add(new DirectedMessage("", "text/plain", new UserAddress("woot")));
  }
}

export let command = new KittieFetcher();

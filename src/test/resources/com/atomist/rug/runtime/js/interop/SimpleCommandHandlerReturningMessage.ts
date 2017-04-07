import {HandleCommand, Instruction, Response, HandlerContext, Plan} from '@atomist/rug/operations/Handlers'
import {CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
class KittieFetcher implements HandleCommand{

  handle(ctx: HandlerContext) : Plan {
    return Plan.ofMessage({body: "Up and at 'em!", kind: "directed", contentType: "text/plain");
  }
}

export let command = new KittieFetcher();

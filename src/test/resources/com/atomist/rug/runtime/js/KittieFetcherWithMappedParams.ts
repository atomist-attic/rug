import {HandleCommand, Instruction, Response, HandlerContext, Plan, ResponseMessage} from '@atomist/rug/operations/Handlers'
import {CommandHandler, Parameter, MappedParameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
@Tags("kitty", "youtube", "slack")
@Intent("show me kitties","cats please")
class KittieFetcher implements HandleCommand{

  @MappedParameter("atomist/repo")
  name: string

  handle(ctx: HandlerContext) : Plan {

    if(this.name != "el duderino") {
      throw new Error("This will not stand");
    }
    let result = new Plan()
    return result;
  }
}

export let command = new KittieFetcher();

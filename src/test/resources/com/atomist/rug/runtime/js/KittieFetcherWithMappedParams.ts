import {HandleCommand, Instruction, Response, HandlerContext, CommandPlan, ResponseMessage} from '@atomist/rug/operations/Handlers'
import {CommandHandler, Parameter, MappedParameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
@Tags("kitty", "youtube", "slack")
@Intent("show me kitties","cats please")
class KittieFetcher implements HandleCommand{

  @MappedParameter("atomist/repo")
  name: string

  handle(ctx: HandlerContext) : CommandPlan {

    if(this.name != "el duderino") {
      throw new Error("This will not stand");
    }
    let result = new CommandPlan()
    return result;
  }
}

export let command = new KittieFetcher();

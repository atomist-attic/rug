import {HandleCommand, MappedParameters, Message, Instruction, Response, HandlerContext, Plan} from '@atomist/rug/operations/Handlers'
import {CommandHandler, Parameter, MappedParameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
@Tags("kitty", "youtube", "slack")
@Intent("show me kitties","cats please")
class KittieFetcher implements HandleCommand{

  @Parameter({description: "his dudeness", pattern: "^.*$"})
  name: string

  @MappedParameter(MappedParameters.REPO_OWNER)
  owner: string

  handle(ctx: HandlerContext) : Plan {
    let pxe = ctx.pathExpressionEngine()

    if(this.name != "el duderino") {
      throw new Error("This will not stand");
    }
    let result = new Plan()
    let message = new Message("KittieFetcher");
    message.addAction({ instruction: {
                 kind: "command",
                 name: "GetKitties"},
                 label: "Fetch'em"})
    result.add(message);
    return result;
  }
}

export let command = new KittieFetcher();
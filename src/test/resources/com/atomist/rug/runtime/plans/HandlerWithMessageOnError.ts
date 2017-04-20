import {HandleCommand, Instruction, Response, HandleResponse, HandlerContext, CommandPlan, ResponseMessage, LifecycleMessage, ChannelAddress} from '@atomist/rug/operations/Handlers'
import {CommandHandler, ResponseHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
@Tags("kitty", "youtube", "slack")
@Intent("show me kitties","cats please")
class KittieFetcher implements HandleCommand {

  handle(ctx: HandlerContext) : CommandPlan {
    let result = new CommandPlan()
    result.add({ instruction: {
                                kind: "execute",
                                name: "ExampleFunction",
                                parameters: {fail: "true", thingy: "rats"}
                              },
                  onError: {kind: "response", body: "oh dear", contentType: "text/plain"}})

    return result;
  }
}

export const command = new KittieFetcher()


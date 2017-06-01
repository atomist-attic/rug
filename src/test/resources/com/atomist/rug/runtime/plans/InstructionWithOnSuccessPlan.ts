import {HandleCommand, Instruction, Response, HandleResponse, HandlerContext, CommandPlan, DirectedMessage, LifecycleMessage, ChannelAddress} from '@atomist/rug/operations/Handlers'
import {CommandHandler, ResponseHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
@Tags("kitty", "youtube", "slack")
@Intent("show me kitties","cats please")
class KittieFetcher implements HandleCommand {

  handle(ctx: HandlerContext) : CommandPlan {
      let onSuccess = new CommandPlan()
      onSuccess.add({ instruction: {
                                  kind: "execute",
                                  name: "ExampleFunction",
                                  parameters: {fail: "false", thingy: "nested success"}
                                }
                    })
    let result = new CommandPlan()
    result.add({ instruction: {
                                kind: "execute",
                                name: "ExampleFunction",
                                parameters: {fail: "false", thingy: "cats"}
                              },
                  onSuccess: onSuccess
                  })

    return result;
  }
}

export const command = new KittieFetcher()


import {HandleCommand, Instruction, Response, HandleResponse, HandlerContext, CommandPlan, DirectedMessage, LifecycleMessage, ChannelAddress} from '@atomist/rug/operations/Handlers'
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
                  onError: {kind: "respond", name: "HandleIt", parameters: this}})

    return result;
  }
}

export const command = new KittieFetcher()

@ResponseHandler("HandleIt", "dummy error handler")
class HandleIt implements HandleResponse<any> {

    handle(response: Response<any>)  {
        return new CommandPlan();
    }
}

export const response = new HandleIt();

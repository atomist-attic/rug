import {HandleCommand, HandleResponse, Instruction, Response, HandlerContext, CommandPlan} from '@atomist/rug/operations/Handlers'
import {CommandHandler, ResponseHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
@Tags("kitty", "youtube", "slack")
@Intent("show me kitties","cats please")
class KittieFetcher implements HandleCommand{

  handle(ctx: HandlerContext) : CommandPlan {

    let result = new CommandPlan()
    result.add({instruction: {kind: "execute", name: "ExampleFunction", parameters: {thingy: "woot"}},
                onSuccess: {name: "SimpleResponseHandler", kind: "respond"} });
    return result;
  }
}

@ResponseHandler("SimpleResponseHandler", "Checks response is equal to passed in parameter")
class Responder implements HandleResponse<String> {
  handle(response: Response<string>) : CommandPlan {
    return new CommandPlan();
  }
}

export let respond = new Responder();

export let command = new KittieFetcher();

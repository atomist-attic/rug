import {HandleCommand, HandleResponse, ResponseMessage, Response, HandlerContext, CommandPlan} from '@atomist/rug/operations/Handlers'
import {CommandHandler, ResponseHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'

@CommandHandler("FunctionKiller","Have a function throw an exception")
class FunctionKiller implements HandleCommand{

  handle(ctx: HandlerContext) : CommandPlan {

    let result = new CommandPlan()
    result.add({instruction: {kind: "execute", name: "ExampleFunction", parameters: {thingy: "woot", exception: true}},});
    return result;
  }
}

export let command = new FunctionKiller();

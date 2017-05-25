import {Respond, Instruction, Response, CommandPlan, HandlerContext} from '@atomist/rug/operations/Handlers'
import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
import {EventHandler, ParseJson, ResponseHandler, CommandHandler, Parameter, Tags, Intent, setScope } from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'
import {HandleResponse, HandleEvent, HandleCommand} from '@atomist/rug/operations/Handlers'

@ResponseHandler("Description")
class SomeHandler implements HandleResponse<any>{

 handle(response: Response<any>, ctx: HandlerContext) : CommandPlan {
    return new CommandPlan();
  }
}

export let kit = setScope(new SomeHandler(), "archive")

import {Respond, Instruction, Response, CommandPlan, HandlerContext} from '@atomist/rug/operations/Handlers'
import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
import {EventHandler, ParseJson, ResponseHandler, CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'
import {HandleResponse, HandleEvent, HandleCommand} from '@atomist/rug/operations/Handlers'

@ResponseHandler("kittyDesc")
class KittiesResponder implements HandleResponse<any>{
 handle(response: Response<any>, ctx: HandlerContext) : CommandPlan {

    if(!ctx.pathExpressionEngine){
        throw new Error("It's got nothing to to with 'nam, Walter!");
    }
    return new CommandPlan();
  }
}

export let kit = new KittiesResponder();

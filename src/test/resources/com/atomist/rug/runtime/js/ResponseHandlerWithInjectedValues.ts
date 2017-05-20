import {Respond, Instruction, Response, CommandPlan, HandlerContext} from '@atomist/rug/operations/Handlers'
import {TreeNode, Match, PathExpression} from '@atomist/rug/tree/PathExpression'
import {EventHandler, ParseJson, ResponseHandler, CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'
import {HandleResponse, HandleEvent, HandleCommand} from '@atomist/rug/operations/Handlers'

@ResponseHandler("Description")
class SomeHandler implements HandleResponse<any>{


 public somenum: number; //doesn't appear in JS
 public something: string = "original";

 handle(response: Response<any>, ctx: HandlerContext) : CommandPlan {

    if(this.somenum === undefined){
        throw new Error("We should have set this")
    }

    if(this.something !== "original"){
        throw new Error("We should not have touched this");
    }

    return new CommandPlan();
  }
}

export let kit = new SomeHandler();

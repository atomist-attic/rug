import {HandleCommand, HandlerContext, CommandPlan} from '@atomist/rug/operations/Handlers'
import {CommandHandler} from '@atomist/rug/operations/Decorators'

@CommandHandler("something")
class HiddenHandler implements HandleCommand {
  handle(ctx: HandlerContext) : CommandPlan {
    const props = this as any;
    for(let p in props){
        if(p === "__description") {
            throw new Error("__description should not be enumerable")
        }
    }
    if(props.__description !== "something"){
        throw new Error("__description should be there for the taking!");
    }
    return new CommandPlan();
  }
}


export const handler = new HiddenHandler();

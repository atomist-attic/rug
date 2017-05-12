import {HandleCommand, HandlerContext, CommandPlan} from '@atomist/rug/operations/Handlers'
import {declareCommandHandler} from '@atomist/rug/operations/Decorators'

class BoringCommand implements HandleCommand {
  handle(ctx: HandlerContext) : CommandPlan {
    return new CommandPlan();
  }
}


export const editor = declareCommandHandler(new BoringCommand(), "description only");
export const editor2 = declareCommandHandler(new BoringCommand(), "description", "andName");

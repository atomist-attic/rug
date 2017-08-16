import { HandleCommand, Instruction, Response, GitHubPullRequest, GitHubBranch, HandlerContext, CommandPlan, DirectedMessage, ResponseMessage, LifecycleMessage, ChannelAddress } from '@atomist/rug/operations/Handlers'
import { CommandHandler, Parameter, Tags, Intent } from '@atomist/rug/operations/Decorators'

@CommandHandler("PresentableWitIdAndParameter")
class PresentableWitIdAndParameter implements HandleCommand {

  handle(ctx: HandlerContext): CommandPlan {
    const result = new CommandPlan();
    const msg = new ResponseMessage("blah");

    msg.addAction({
      instruction: {
        kind: "command",
        name: "somecommand",
      },
      id: "someid",
      parameterName: "paramName",
    });
    result.add(msg);

    return result;
  }
}

export const command = new PresentableWitIdAndParameter()


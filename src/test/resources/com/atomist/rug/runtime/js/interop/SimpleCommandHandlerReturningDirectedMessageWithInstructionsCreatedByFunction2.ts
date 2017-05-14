import {
  CommandHandler,
  Intent,
  Parameter,
  Tags
  } from "@atomist/rug/operations/Decorators";
import {
  ChannelAddress,
  CommandPlan,
  DirectedMessage,
  HandleCommand,
  HandlerContext,
  Instruction,
  Response
  } from "@atomist/rug/operations/Handlers";

import {createCommandHandler} from "@atomist/rug/operations/HandlerUtils"


class KittieFetcher implements HandleCommand {

  handle(ctx: HandlerContext): CommandPlan {
    const plan = new CommandPlan();
    const message = new DirectedMessage("some message", new ChannelAddress("general"));
    message.addAction({
      instruction: {
        kind: "command",
        name: "GetKitties"
      },
      id: "123"
    })
    plan.add(message);
    return plan;
  }
}

export let command = [ createCommandHandler( "ShowMeTheKitties", "kitties", new KittieFetcher().handle )];

import {HandleCommand, Instruction, Response, HandlerContext, Plan, Message} from '@atomist/rug/operations/Handlers'
import {CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
import {Build} from "@atomist/rug/cortex/stub/Build"

@CommandHandler("SimpleCommandHandlerWithBadStubUse",
    "Deliberate error")
class SimpleCommandHandlerWithBadStubUse implements HandleCommand{

  handle(ctx: HandlerContext) {
      // This will fail
      let build = new Build().repo();
      return Plan.ofMessage(new Message("foobar"));
  }
}

export let command = new SimpleCommandHandlerWithBadStubUse();

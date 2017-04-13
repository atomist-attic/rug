import {HandleCommand, Instruction, Response, HandlerContext, Plan, DirectedMessage, UserAddress} from '@atomist/rug/operations/Handlers'
import {CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
import {Build} from "@atomist/rug/cortex/stub/Build"

let config = {description: "desc", pattern: "@any"}

@CommandHandler("SameConfig", "Uses same config object")
class SameConfig implements HandleCommand{

  @Parameter(config)
  foo: number

  @Parameter(config)
  bar: number

  handle(ctx: HandlerContext) {
      return new Plan();
  }
}

export let command = new SameConfig();

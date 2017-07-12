import {HandleCommand, Instruction, Response, HandlerContext, CommandPlan, DirectedMessage, UserAddress} from '@atomist/rug/operations/Handlers'
import {CommandHandler, MappedParameter, declareMappedParameter, Parameter, declareParameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
import {Build} from "@atomist/rug/cortex/stub/Build"

let config = {description: "desc", pattern: "@any"}

@CommandHandler("Uses same config object")
class ParentHandler implements HandleCommand{

  @Parameter(config)
  foo: number

  @Parameter(config)
  bar: number

  @MappedParameter("blah-parent")
  blah: string

  @MappedParameter("quz-parent")
  quz: string

  handle(ctx: HandlerContext) {
      return new CommandPlan();
  }
}

export let parent = new ParentHandler();


@CommandHandler("Child with overrides")
class ChildHandler extends ParentHandler {

  @Parameter({description: "child", pattern: "@any"})
  foo: number

  @Parameter(config)
  baz: number

  @MappedParameter("blah-child")
  blah: string

  @MappedParameter("really-child")
  really: string

  handle(ctx: HandlerContext) {
      return new CommandPlan();
  }
}
const child = new ChildHandler();
declareParameter(child, "baz", {description: "dup", pattern: "@any"})
declareMappedParameter(child, "really", "manual")

export { child };


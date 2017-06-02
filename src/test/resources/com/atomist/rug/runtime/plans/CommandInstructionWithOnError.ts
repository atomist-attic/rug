import { HandleCommand, HandlerContext, ResponseMessage, CommandPlan, DirectedMessage, ChannelAddress } from '@atomist/rug/operations/Handlers';
import { CommandHandler, Parameter, Tags, Intent } from '@atomist/rug/operations/Decorators';
import { Pattern } from '@atomist/rug/operations/RugOperation';

/**
 * A Simple handler to test path expression.
 */
@CommandHandler("KafkaTest", "Simple handler to test path expression")
@Tags("documentation")
@Intent("test")
export class KafkaTest implements HandleCommand {

    handle(ctx: HandlerContext): CommandPlan {
        let plan = new CommandPlan();
        plan.add({
            instruction: {
                kind: "command",
                name: "PathExpressionTest"
            },
            onError: new DirectedMessage("test", new ChannelAddress("test")),
        });
        return plan;
    }
}


export const kafka = new KafkaTest();

/**
 * A Simple handler to test path expression.
 */
@CommandHandler("PathExpressionTest", "Simple handler to test path expression")
@Tags("documentation")
@Intent("test")
export class PathExpressionTest implements HandleCommand {

    handle(ctx: HandlerContext): CommandPlan {
        let plan = new CommandPlan();
        throw new Error("blah");
        return plan;
    }
}

export const pathExpressionTest = new PathExpressionTest();

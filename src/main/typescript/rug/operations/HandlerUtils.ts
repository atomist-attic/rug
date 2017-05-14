import { Match } from "../tree/PathExpression";
import { HandlerContext, CommandPlan, EventPlan } from "./Handlers";

/**
 * Create an event handler with the given expression
 */
export function createEventHandler(name: string, description: string,
                                   expr: any, handle: (root: Match<any, any>) => EventPlan,
                                   tags: string[] = []) {
    return {
        __kind: "event-handler",
        __name: name,
        __description: description,
        __expression: (typeof expr === "string") ? expr : expr.expression,
        __tags: tags,
        handle,
    };
}

export function createCommandHandler(name: string, description: string,
                                     handle: (HandlerContext) => CommandPlan,
                                     tags: string[] = [],
                                     intent: string[] = []) {
    return {
        __kind: "command-handler",
        __name: name,
        __description: description,
        __tags: tags,
        __intent: intent,
        handle,
    };
}

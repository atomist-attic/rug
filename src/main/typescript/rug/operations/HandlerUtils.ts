import { Match } from "../tree/PathExpression";
import { EventPlan } from "./Handlers";

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

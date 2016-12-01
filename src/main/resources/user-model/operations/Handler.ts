
import {Match,PathExpression} from "../tree/PathExpression"
import {Project,File} from "../model/Core"

/**
 * Central Atomist callback interface. Allows strongly
 * typed responses to Atomist events.
 */
interface CallbackRegistry {

  on<R,N>(pathExpression: string, handler: (m: Match<R,N>) => void): void
}

// Also call unicode like  π if we want some of $ familiarity
export interface Atomist extends CallbackRegistry {

}


import {Match,PathExpression} from "../tree/PathExpression"
import {Project,File,Service} from "../model/Core"

/**
 * Central Atomist callback interface. Allows strongly
 * typed responses to Atomist events.
 */
interface CallbackRegistry {

  on<R,N>(pathExpression: string, handler: (m: ContextMatch<R,N>) => void): void

  on<R,N>(pathExpression: PathExpression<R,N>, handler: (m: ContextMatch<R,N>) => void): void

}

export interface ContextMatch<R,N> extends Match<R,N> {

  serviceSource: ServiceSource
}


// Also call unicode like  π if we want some of $ familiarity
export interface Atomist extends CallbackRegistry {

}


/**
 * Exposes model information to us
 */
export interface ServiceSource {

  services(): Array<Service>
}
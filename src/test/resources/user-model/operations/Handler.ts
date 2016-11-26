
import {Match,PathExpression} from "../tree/PathExpression"

import {Project,File} from "../model/Core"

/**
 * Central Atomist callback interface. Allows strongly
 * typed responses to Atomist events.
 */
interface CallbackRegistry {

  on<R,N>(pathExpression: string, handler: (m: Match<R,N>) => void): void
}

module Atomist {

    export let registry: CallbackRegistry = null
}

Atomist.registry.on<Project,File>("/src/main/**.java", m => null)
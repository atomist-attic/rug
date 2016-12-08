
import {Match,PathExpression,TreeNode} from "../tree/PathExpression"
import {Project,File,Service} from "../model/Core"

/**
 * Central Atomist callback interface. Allows strongly
 * typed responses to Atomist events.
 */
interface CallbackRegistry {

  on<R,N>(pathExpression: string, handler: (m: ContextMatch<R,N>) => void): void

  on<R,N>(pathExpression: PathExpression<R,N>, handler: (m: ContextMatch<R,N>) => void): void

}

export interface ContextMatch<R,N> extends Match<R,N>, ServiceSource {

  serviceSource: ServiceSource
}


// Also call unicode like  π if we want some of $ familiarity
export interface Atomist extends CallbackRegistry {
  messageBuilder(): MessageBuilder
}


/**
 * Exposes model information to us
 */
export interface ServiceSource {

  services(): Array<Service>

  messageBuilder(): MessageBuilder

}


export interface MessageBuilder {

 regarding(n: TreeNode, teamId: string): Message

 say(msg: string, teamId: string): Message

}


export interface Message {

 withAction(s: string): Message

 address(channelId: String): Message

 send(): void

}
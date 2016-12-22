
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


/**
 * Enables us to create new messages that will be routed
 * appropriately by the infrastructure
 */
export interface MessageBuilder {

 regarding(n: TreeNode): Message

 say(msg: string): Message

}

export interface ActionRegistry {

  findByName(name: String): Action
}

export interface Message {

  actionRegistry(): ActionRegistry

/**
 * Set the message
 */
 say(msg: string): Message

 withAction(a: Action): Message

 withActionNamed(a: String): Message

 address(channelId: string): Message

/**
 * Set the address (channel)
 */
 on(channelId: string): Message

 send(): void

}


export interface Action {

  title(): string
  callback(): Callback
  parameters(): Array<ParameterValue>

}

export interface ParameterValue {

  name: string
  value: any
}

export interface Callback {

  callbackType(): string
  rug(): RugArchive

}

export interface RugArchive {

  group: string
  artifact: string
  name: string
  version: string

}

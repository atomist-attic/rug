
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

  userMessageRouter(): UserMessageRouter

}

export interface UserMessageRouter {

  /**
    * Send a message to the channel associated with the current service,
    * or `general` or some other fallback channel if the service is null
    *
    * @param service service we're concerned with. Null if this affects multiple services
    * @param msg     message
    */
 messageServiceChannel(service: Service, msg: string): void

  /**
    * Send a message to the current user, possibly associated with a current service.
    *
    * @param service    service we're concerned with. Null if this affects multiple services
    * @param screenName screen name in chat
    * @param msg        message
    */
 messageUser(service: Service, screenName: string, msg: string): void

  /**
    * Send a message to the given channel
    *
    * @param channelName name of the channel
    * @param msg         message text
    */
 messageChannel(channelName: String, msg: string): void
}



interface MessageBuilder {

 regarding(n: TreeNode, teamId: string): Message

 say(msg: string, teamId: string): Message

}


interface Message {

 withAction(s: string): Message

 send(): void

}
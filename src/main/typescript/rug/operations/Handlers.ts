import {
  GraphNode,
  Match,
  PathExpression,
  PathExpressionEngine,
  TreeNode,
} from "../tree/PathExpression";
import { Parameter } from "./RugOperation";

export interface RugCoordinate {
  readonly name: string;
  readonly group: string;
  readonly artifact: string;
}

type InstructionKind = "generate" | "edit" | "execute" | "respond" | "command";

export interface Instruction<T extends InstructionKind> {
  readonly name: string | RugCoordinate;
  readonly parameters?: {};
  readonly kind: T;
}

export class EventRespondable<T extends Edit | Generate | Execute> {
  public instruction: T;
  public onSuccess?: EventPlan | EventMessage | Respond;
  public onError?: EventPlan | EventMessage | Respond;
}

export class CommandRespondable<T extends Edit | Generate | Execute | Command> {
  public instruction: T;
  public onSuccess?: CommandPlan | CommandMessage | Respond;
  public onError?: CommandPlan | CommandMessage | Respond;
}

export class Presentable<T extends InstructionKind> {
  public instruction: Instruction<T> | PresentableGenerate | PresentableEdit;
  public label?: string;
  public id?: string;
}

export class Identifiable<T extends InstructionKind> {
  public instruction: Instruction<T> | PresentableGenerate | PresentableEdit;
  public id?: string;
}

// Location to a project.
// in the future, we could add things like github urls, orgs etc.
// tslint:disable-next-line:no-empty-interface
interface ProjectReference { }

export interface ProjectInstruction<T extends InstructionKind> extends Instruction<T> {
  project: string | ProjectReference;
}

type EditorTargetKind = "direct" | "github-pull-request" | "github-branch";
/**
 * How should the `edit` be applied? PR details etc.
 */
export interface EditorTarget<T extends EditorTargetKind> {
  kind: T;
  baseBranch: string;
}

/**
 * Get an editor instruction to run a GitHub Pull Request
 */
export class GitHubPullRequest implements EditorTarget<"github-pull-request"> {
  public kind: "github-pull-request" = "github-pull-request";
  public title?: string; // title of the PR
  public body?: string; // body of the PR (defaults to editor changelog)
  public headBranch?: string; // name of PR source branch (default auto-generated)
  constructor(public baseBranch: string = "master") { }
}

/**
 * Get an editor instruction to run on a new GitHub branch
 */
export class GitHubBranch implements EditorTarget<"github-branch"> {
  public kind: "github-branch" = "github-branch";
  constructor(public baseBranch: string = "master", public headBranch?: string) { }
}
// tslint:disable-next-line:no-empty-interface
export interface Edit extends ProjectInstruction<"edit"> {
  target?: EditorTarget<EditorTargetKind>;
  commitMessage?: string;
}

// extends ProjectInstruction because we need to know the project name
// tslint:disable-next-line:no-empty-interface
export interface Generate extends ProjectInstruction<"generate"> { }

// because in a message, we may not know project name yet
export interface PresentableGenerate extends Instruction<"generate"> {
  project?: string | ProjectReference;
}

// because in a message, we may not know project name yet
export interface PresentableEdit extends Instruction<"edit"> {
  project?: string | ProjectReference;
}

// tslint:disable-next-line:no-empty-interface
export interface Execute extends Instruction<"execute"> { }
// tslint:disable-next-line:no-empty-interface
export interface Respond extends Instruction<"respond"> { }
// tslint:disable-next-line:no-empty-interface
export interface Command extends Instruction<"command"> { }

export interface HandleCommand {
  handle(ctx: HandlerContext): CommandPlan;
}

export interface HandleEvent<R extends GraphNode, M extends GraphNode> {
  handle(root: Match<R, M>): EventPlan;
}

export interface HandleResponse<T> {
  handle(response: Response<T>, ctx?: HandlerContext): EventPlan | CommandPlan;
}

/**
 * Context available to all handlers. Unique to a team.
 * Exposes a context root from which queries can be run,
 * using the given PathExpressionEngine
 */
export interface HandlerContext {

  /**
   * Id of the team we're working on behalf of
   */
  teamId: string;

  pathExpressionEngine: PathExpressionEngine;

  /**
   * The root of this team's context. Allows execution
   * of path expressions.
   */
  contextRoot: GraphNode;
}

export enum Status {
  failure,
  success,
}

export interface Response<T> {
  msg: string;
  code: number;
  status: Status;
  body: T;
}

type Respondable = EventRespondable<any> | CommandRespondable<any>;

/**
 * A bunch of stuff to do asynchronously
 * PlanMessages got to the bot.
 * ImmediatelyRunnables are run straight away
 */
export abstract class Plan {

  public instructions: Respondable[] = [];

  public messages: PlanMessage[] = [];

  public add?(thing: Respondable | PlanMessage): this {
    if (thing instanceof ResponseMessage || thing instanceof DirectedMessage || thing instanceof LifecycleMessage) {
      this.messages.push(thing);
    } else {
      this.instructions.push(thing);
    }
    return this;
  }
}

type EventMessage = LifecycleMessage | DirectedMessage;

type CommandMessage = ResponseMessage | DirectedMessage;

/**
 * For returning from Event Handlers
 */
export class EventPlan extends Plan {

  public static ofMessage(m: EventMessage): EventPlan {
    return new EventPlan().add(m);
  }

  public add?(msg: EventMessage | EventRespondable<any>): this {
    return super.add(msg);
  }
}

/**
 * Plans returned from Command Handlers
 */
export class CommandPlan extends Plan {

  public static ofMessage(m: CommandMessage): CommandPlan {
    return new CommandPlan().add(m);
  }

  public add?(msg: CommandMessage | CommandRespondable<any>): this {
    return super.add(msg);
  }
}

export type MessageMimeType = "application/x-atomist-slack+json" | "text/plain";

export abstract class MessageMimeTypes {
  public static SLACK_JSON: MessageMimeType = "application/x-atomist-slack+json";
  public static PLAIN_TEXT: MessageMimeType = "text/plain";
}

type MessageKind = "response" | "lifecycle" | "directed";

interface Message<T extends MessageKind> {
  kind: T;
}

abstract class LocallyRenderedMessage<T extends MessageKind> implements Message<T> {
  public kind: T;

  public usernames?: string[] = [];
  public channelNames?: string[] = [];

  public contentType: MessageMimeType = MessageMimeTypes.PLAIN_TEXT;
  public body: string;
  public instructions?: Array<Identifiable<any>> = [];

  public addAddress?(address: MessageAddress): this {
    if (address instanceof UserAddress) {
      this.usernames.push(address.username);
    } else {
      this.channelNames.push(address.channelName);
    }
    return this;
  }

  public addAction?(instruction: Identifiable<any>): this {
    this.instructions.push(instruction);
    return this;
  }
}

/**
 * Represents the response to the bot from a command
 */
export class ResponseMessage extends LocallyRenderedMessage<"response"> {
  public kind: "response" = "response";

  constructor(body: string, contentType?: MessageMimeType) {
    super();
    this.body = body;
    if (contentType) {
      this.contentType = contentType;
    }
  }
}

export class UserAddress {
  constructor(public username: string) { }
}

export class ChannelAddress {
  constructor(public channelName: string) { }
}

export type MessageAddress = UserAddress | ChannelAddress;

/**
 * Uncorrelated message to the bot
 */
export class DirectedMessage extends LocallyRenderedMessage<"directed"> {
  public kind: "directed" = "directed";

  constructor(body: string, address: MessageAddress, contentType?: MessageMimeType) {
    super();
    this.body = body;
    if (contentType) {
      this.contentType = contentType;
    }
    this.addAddress(address);
  }
}

/**
 * Correlated, updatable messages to the bot
 */
export class LifecycleMessage implements Message<"lifecycle"> {

  public kind: "lifecycle" = "lifecycle";
  public node: GraphNode;
  public instructions?: Array<Presentable<any>> = [];
  public lifecycleId: string;

  constructor(node: GraphNode, lifecycleId: string) {
    this.node = node;
    this.lifecycleId = lifecycleId;
  }

  public addAction?(instruction: Presentable<any>): this {
    this.instructions.push(instruction);
    return this;
  }
}

export type PlanMessage = ResponseMessage | DirectedMessage | LifecycleMessage;

export abstract class MappedParameters {
  public static readonly GITHUB_REPO_OWNER: string = "atomist://github/repository/owner";
  public static readonly GITHUB_REPOSITORY: string = "atomist://github/repository";
  public static readonly SLACK_CHANNEL: string = "atomist://slack/channel";
  public static readonly SLACK_TEAM: string = "atomist://slack/team";
  public static readonly SLACK_USER: string = "atomist://slack/user";
  public static readonly GITHUB_WEBHOOK_URL: string = "atomist://github_webhook_url";
}

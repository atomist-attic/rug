
import {GraphNode} from "@atomist/rug/tree/PathExpression"

export class Commit implements GraphNode {

    private _madeBy: Person = null

    nodeName(): string {  return "Commit" }

    // Node we need -dynamic to allow dispatch in the proxy
    nodeTags(): string[] { return [ "Commit", "-dynamic" ] }

    withMadeBy(p: Person): Commit {
        this._madeBy = p 
        return this
    }

    madeBy(): Person { return this._madeBy }

}

export class Person implements GraphNode {

    private _gitHubId: GitHubId = null

    constructor(private _name: string) {}

    nodeName(): string {  return this._name }

    nodeTags(): string[] { return [ "Person", "-dynamic" ] }

    name(): string {  return this._name }

    gitHubId() { return this._gitHubId } 

    withGitHubId(g: GitHubId): Person {
        this._gitHubId = g
        return this
    }

}

export class GitHubId implements GraphNode {

    constructor(private _id: string) {}

    nodeName(): string {  return this._id }

    nodeTags(): string[] { return [ "GitHubId", "-dynamic" ] }

    id(): string {  return this._id }

}
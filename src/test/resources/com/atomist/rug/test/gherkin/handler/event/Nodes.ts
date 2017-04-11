/**
 * Test nodes for use in handler tests. 
 * These are NOT realistic, and do not represent our actual domain model.
 */

import {GraphNode} from "@atomist/rug/tree/PathExpression"

export class Commit implements GraphNode {

    private _madeBy: Person

    private _sha: string

    nodeName(): string {  return "Commit" }

    // Node we need -dynamic to allow dispatch in the proxy
    nodeTags(): string[] { return [ "Commit", "-dynamic" ] }

    withMadeBy(p: Person): Commit {
        this._madeBy = p 
        return this
    }

    withSha(sha: string): Commit {
        this._sha = sha
        return this;
    }

    get sha() { return this._sha }

    get madeBy(): Person { return this._madeBy }

}

export class Person implements GraphNode {

    private _gitHubId: GitHubId = null

    constructor(private _name: string) {} 

    // Intentionally make this different to the name, to test
    nodeName(): string {  return "A person" }

    nodeTags(): string[] { return [ "Person", "-dynamic" ] }

    get name(): string {  return this._name }

    get gitHubId() { return this._gitHubId }

    withGitHubId(g: GitHubId): Person {
        this._gitHubId = g
        return this
    }

}

export class GitHubId implements GraphNode {

    constructor(private _id: string) {}

    nodeName(): string {  return this._id }

    nodeTags(): string[] { return [ "GitHubId", "-dynamic" ] }

    get id(): string {  return this._id }

}
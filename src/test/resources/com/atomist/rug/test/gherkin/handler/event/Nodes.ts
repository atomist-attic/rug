/**
 * Test nodes for use in handler tests. 
 * These are NOT realistic, and do not represent our actual domain model.
 */

import {GraphNode} from "@atomist/rug/tree/PathExpression"
import {AddressedNodeSupport} from "@atomist/rug/tree/AddressedNodeSupport"

export class Commit extends AddressedNodeSupport implements GraphNode {

    private _madeBy: Person = null

    nodeName(): string {  return "Commit" }

    // Node we need -dynamic to allow dispatch in the proxy
    nodeTags(): string[] { return [ "Commit", "-dynamic" ] }

    withMadeBy(p: Person): Commit {
        this._madeBy = p 
        p.navigatedFrom(this, "/madeBy")
        return this
    }

    madeBy(): Person { return this._madeBy }

}

export class Person extends AddressedNodeSupport implements GraphNode {

    private _gitHubId: GitHubId = null

    constructor(private _name: string) { super() }

    nodeName(): string {  return this._name }

    nodeTags(): string[] { return [ "Person", "-dynamic" ] }

    name(): string {  return this._name }

    gitHubId() { return this._gitHubId } 

    withGitHubId(g: GitHubId): Person {
        this._gitHubId = g
        g.navigatedFrom(this, "/gitHubId")
        return this
    }

}

export class GitHubId extends AddressedNodeSupport implements GraphNode {

    constructor(private _id: string) { super() }

    nodeName(): string {  return this._id }

    nodeTags(): string[] { return [ "GitHubId", "-dynamic" ] }

    id(): string {  return this._id }

}
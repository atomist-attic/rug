/**
 * Test nodes for use in handler tests. 
 * These are NOT realistic, and do not represent our actual domain model.
 */

import {GraphNode, Addressed} from "@atomist/rug/tree/PathExpression"

export abstract class AddressedNode implements Addressed {

    private _address: string = ""

    address() { return this._address }

    setAddress(addr: string) {
        this._address = addr
    }
    
}

export class Commit extends AddressedNode implements GraphNode {

    private _madeBy: Person = null

    nodeName(): string {  return "Commit" }

    // Node we need -dynamic to allow dispatch in the proxy
    nodeTags(): string[] { return [ "Commit", "-dynamic" ] }

    withMadeBy(p: Person): Commit {
        this._madeBy = p 
        p.setAddress(this.address() + "/" + "madeBy")
        return this
    }

    madeBy(): Person { return this._madeBy }

}

export class Person extends AddressedNode implements GraphNode {

    private _gitHubId: GitHubId = null

    constructor(private _name: string) { super() }

    nodeName(): string {  return this._name }

    nodeTags(): string[] { return [ "Person", "-dynamic" ] }

    name(): string {  return this._name }

    gitHubId() { return this._gitHubId } 

    withGitHubId(g: GitHubId): Person {
        this._gitHubId = g
        g.setAddress(this.address() + "/" + "gitHubId")
        return this
    }

}

export class GitHubId extends AddressedNode implements GraphNode {

    constructor(private _id: string) { super() }

    nodeName(): string {  return this._id }

    nodeTags(): string[] { return [ "GitHubId", "-dynamic" ] }

    id(): string {  return this._id }

}
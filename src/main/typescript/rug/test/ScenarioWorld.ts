/*
 * Copyright © 2017 Atomist, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Project } from "../model/Project";

/**
 * Superinterface for all worlds: Isolated contexts
 * available during scenario execution
 */
export interface ScenarioWorld {

    /**
     * Get the value of the given key in the scenario, or null
     */
    get(key: string): any;

    /**
     * Bind the value to the given key in the scenario
     */
    put(key: string, what: any): void;

    /**
     * Clear the value of the given key in the scenario
     */
    clear(key: string): void;

    /**
     * Abort execution of the current scenario. This will cause failure.
     */
    abort(): void;

    /**
     * Was execution of the current scenario aborted?
     */
    aborted(): boolean;

    /**
     * Did the last operation fail due to invalid parameters?
     * Otherwise null
     */
    invalidParameters(): any;

    /**
     * Clone the given repo and provide a reference to the project
     */
    cloneRepo(cloneInfo: CloneInfo): Project;

}

/**
 * Identifies a repo
 */
export interface RepoId {

    branch: string;

    sha: string;

    owner: string;

    name: string;

}

/**
 * Information necessary to clone a repo
 */
export class CloneInfo implements RepoId {

    public branch = "master";

    public sha: string;

    public depth: number = 10;

    public remote: string;

    constructor(public owner: string, public name: string) { }

    public withBranch(branch: string): CloneInfo {
        this.branch = branch;
        return this;
    }

    public withSha(sha: string): CloneInfo {
        this.sha = sha;
        return this;
    }

    public withDepth(depth: number): CloneInfo {
        this.depth = depth;
        return this;
    }

    public withRemote(remote: string): CloneInfo {
        this.remote = remote;
        return this;
    }
}

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

import { Project } from "../../model/Project";
import { CommandPlan, EventPlan, Plan } from "../../operations/Handlers";
import { GraphNode } from "../../tree/PathExpression";
import { Result } from "../Result";
import { RepoId, ScenarioWorld } from "../ScenarioWorld";

/**
 * All handler scenario worlds expose the plan,
 * if constructed by code under test.
 */
export interface HandlerScenarioWorld<T extends Plan> extends ScenarioWorld {

    /**
     * Return an empty project. Useful in creating fake repos.
     */
    emptyProject(name: string): Project;

    /**
     * Return a distinct project instance that contains all the files from the initial project.
     */
    projectStartingWith(project: Project): Project;

    /**
     * Add this node to the root context so it can
     * be matched by path expressions other than event handler expressions
     */
    addToRootContext(n: GraphNode): void;

    setRootContext(n: GraphNode): void;

    /**
     * May be null if setRootContext has not been called
     */
    getRootContext(): GraphNode;

    /**
     * Define the given repo
     */
    defineRepo(owner: string, name: string, branchOrSha: string, p: Project);

    defineRepo(repoId: RepoId, p: Project);

    /**
     * Return a single plan. Throws an exception if no plan was recorded
     */
    plan(): T;

    /**
     * Return the plan recorded for the handler with this name, or null
     */
    planFor(handlerName: string): T;

    /**
     * Return the number of plans recorded for this scenario
     */
    planCount(): number;

    /**
     * Are all plan(s) internally valid?
     * Do the referenced commands and project operations exist?
     */
    planIsInternallyValid(): boolean;

}

/**
 * Subinterface of ScenarioWorld specific to
 * scenarios testing project operations
 */
export interface CommandHandlerScenarioWorld extends HandlerScenarioWorld<CommandPlan> {

    /**
     * Return the CommandHandler with the given name, or null if none is found.
     * Pass to invokeHandler
     */
    commandHandler(name: string): any;

    /**
     * Execute the given handler, validating parameters
     */
    invokeHandler(commandHandler: any, params?: {});

}

export interface EventHandlerScenarioWorld extends HandlerScenarioWorld<EventPlan> {

    /**
     * Register the named handler to respond to input
     * Return the handler, or null if none is found.
     */
    registerHandler(name: string): any;

    /**
     * Publish the given event. Should be materialized
     */
    sendEvent(n: GraphNode): void;
}

// Callback for given and when steps
type SetupCallback = (HandlerScenarioWorld, ...args) => void;

type ThenCallback = (HandlerScenarioWorld?, ...args) => Result | boolean | void;

interface Definitions {

    Given(s: string, f: SetupCallback): void;

    When(s: string, f: SetupCallback): void;

    Then(s: string, f: ThenCallback): void;

}

// Registered with Nashorn by the test runner
declare const com_atomist_rug_test_gherkin_GherkinRunner$_definitions: Definitions;

export function Given(s: string, f: SetupCallback) {
    com_atomist_rug_test_gherkin_GherkinRunner$_definitions.Given(s, f);
}

export function When(s: string, f: SetupCallback) {
    com_atomist_rug_test_gherkin_GherkinRunner$_definitions.When(s, f);
}

/**
 * A Then step can return a Result object, containing a result and details,
 * a boolean indicating pass or fail, or void.
 * A successful void return is equivalent to true, while throwing an Error
 * means failure. The void return style allows idiomatic use of assertion frameworks
 * such as chai.
 */
export function Then(s: string, f: ThenCallback) {
    com_atomist_rug_test_gherkin_GherkinRunner$_definitions.Then(s, f);
}

/**
 * Convenient assertion if you are not using an assertion framework
 * such as chai
 */
export function rugAssert(statement: () => boolean, message: string) {
    if (!statement.apply(null)) {
        throw new Error(`Assertion failed: ${message}. Code was [${statement}]`);
    }
}

export function rugAssertEqual(a, b) {
    if (!(a === b)) {
        throw new Error(`Assertion failed: ${a} did not equal ${b}`);
    }
}

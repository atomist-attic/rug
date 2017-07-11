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
import { Result } from "../Result";
import { EditProject } from "../../operations/ProjectEditor"
import { PopulateProject } from "../../operations/ProjectGenerator"
import { ScenarioWorld } from "../ScenarioWorld";

/**
 * Subinterface of ScenarioWorld specific to
 * scenarios testing project operations
 */
export interface ProjectScenarioWorld extends ScenarioWorld {

    /**
     * Set the project fixture
     */
    setProject(p: Project): void;

    /**
     * Return a project editor from the local context identified by name
     */
    editor(name: string): EditProject;

    /**
     * Return a project generator from the local context identified by name
     */
    generator(name: string): PopulateProject;

    /**
     * Edit the project with the given editor, validating parameters
     */
    editWith(ed: EditProject, params?: {});

    /**
     * Create a project using the given generator named projectName, validating parameters
     */
    generateWith(gen: PopulateProject, projectName: string, params?: {});

    /**
     * Did the editor make modifications in this scenario?  Note
     * the initial population of a project prior to entering the
     * generator 'populate' method does not contribute to the number
     * of modifications returned by this method.
     */
    modificationsMade(): boolean;

    /**
     * Did editing fail?
     */
    failed(): boolean;

    /**
     * How many editors were run in the execution of this scenario?
     */
    editorsRun(): number;
}

// Callback for given and when steps
type SetupCallback = (Project, ProjectScenarioWorld?, ...args) => void;

type ThenCallback = (Project, ProjectScenarioWorld?, ...args) => Result | boolean | void;

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

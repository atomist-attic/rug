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

import { Result } from "../Result";

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

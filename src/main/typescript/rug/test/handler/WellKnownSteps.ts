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

import {
    CommandHandlerScenarioWorld, EventHandlerScenarioWorld,
} from "./Core";

import { Given, Then, When } from "./Steps";

// Register well-known steps

/**
 * Nothing.  Typical starting point for command handler testing.
 */
Given("nothing", (w: CommandHandlerScenarioWorld) => {
    return;
});

/**
 * Generic event handler registrtion.
 */
Given("([a-zA-Z0-9]+) handler", (w: EventHandlerScenarioWorld, handlerName: string) => {
    w.registerHandler(handlerName);
});

/**
 * No event handler was triggered.
 */
Then("no handler fired", (w: EventHandlerScenarioWorld) => {
    return w.plan() === null;
});

/**
 * Valid command handler parameters.
 */
Then("handler parameters were valid", (w: CommandHandlerScenarioWorld) => {
    console.log("Registering params are valid then");
    return w.invalidParameters() === null;
});

/**
 * Invalid command handler parameters.
 */
Then("handler parameters were invalid", (w: CommandHandlerScenarioWorld) => {
    return w.invalidParameters() !== null;
});

/**
 * The returned plan has no messages.
 */
Then("plan has no messages", (world: CommandHandlerScenarioWorld | EventHandlerScenarioWorld) => {
    return world.plan().messages.length === 0;
});

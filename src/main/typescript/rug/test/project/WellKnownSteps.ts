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

import { Given, ProjectScenarioWorld, Then, When } from "./Core";

import { CloneInfo } from "../ScenarioWorld";

import { Project } from "../../model/Project";

// Register well-known steps

/**
 * Empty project.
 */
Given("an empty project", (p: Project) => {
    return;
});

/**
 * Cloned content from GitHub.
 */
Given("github ([^/]+)/([^/]+)",
    (p: Project, w: ProjectScenarioWorld, owner: string, name: string) => {
        const repo = new CloneInfo(owner, name);
        const project = w.cloneRepo(repo);
        w.setProject(project);
    },
);

/**
 * Cloned branch from GitHub.
 */
Given("github ([^/]+)/([^/]+)/([^/]+)",
    (p: Project, w: ProjectScenarioWorld, owner: string, name: string, branch: string) => {
        const repo = new CloneInfo(owner, name).withBranch(branch);
        const project = w.cloneRepo(repo);
        w.setProject(project);
    },
);

/**
 * The entire contents of the Rug archive project.
 */
Given("the archive root", (p: Project) => {
    p.copyEditorBackingFilesPreservingPath("");
});

/**
 * The contents of this archive, excluding Atomist content.
 */
Given("archive non Atomist content", (p: Project) => {
    p.copyEditorBackingProject();
});

/**
 * Editor made changes.
 */
Then("changes were made", (p: Project, w: ProjectScenarioWorld) => {
    return w.modificationsMade();
});

/**
 * Editor made NoChange.
 */
Then("no changes were made", (p: Project, w: ProjectScenarioWorld) => {
    return !w.modificationsMade();
});

/**
 * Valid parameters.
 */
Then("parameters were valid", (p: Project, w: ProjectScenarioWorld) => {
    return w.invalidParameters() === null;
});

/**
 * Invalid parameters.
 */
Then("parameters were invalid", (p: Project, w: ProjectScenarioWorld) => {
    return w.invalidParameters() !== null;
});

/**
 * Generic file existence check.
 */
Then("file at ([^ ]+) should exist", (p: Project, w: ProjectScenarioWorld, path: string) => {
    return p.fileExists(path);
});

/**
 * Generic file content check.
 */
Then("file at ([^ ]+) should contain (.*)",
    (p: Project, w: ProjectScenarioWorld, path: string, searchString: string) => {
        return p.fileContains(path, searchString);
    },
);

/**
 * When step should fail.
 */
Then("it should fail", (p: Project, w: ProjectScenarioWorld) => {
    w.failed();
});

/**
 * The scenario was aborted due to an exception being thrown.
 */
Then("the scenario aborted", (p: Project, w: ProjectScenarioWorld) => {
    w.aborted();
});

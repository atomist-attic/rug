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

import { File, Project } from "../../model/Core";

/**
 * Pretty list the files in project
 */
export function prettyListFiles(p: Project): string {
    return `${p.totalFileCount} files. Files:\n\t${p.files
        .map(fileSummary)
        .join("\n\t")
        }`;
}

function fileSummary(f: File): string {
    return `file:[${f.path}];length:${f.contentLength}`;
}

function dumpFile(f: File): string {
    return `${fileSummary(f)}\n[${f.content}]`;
}

/**
 * Dump the path of this file followed by the content
 */
export function dump(p: Project, path: string): string {
    // TODO what if it's a directory?
    const f = p.findFile(path);
    if (f) {
        return dumpFile(f);
    } else {
        return `What is the sound of one hand clapping: Nothing found at [${path}]`;
    }
}

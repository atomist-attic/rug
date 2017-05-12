
import { Project } from "../model/Core";

/**
 * Use to load projects backed by git repositories.
 */
export interface GitProjectLoader {

    /**
     * Resolve the latest in the given branch. Throw exception if not found.
     */
     loadBranch(owner: string, repoName: string, branch: string): Project;

    /**
     * Resolve the tree for this sha. Throw exception if not found.
     */
    loadSha(owner: string, repoName: string, sha: string): Project;

}
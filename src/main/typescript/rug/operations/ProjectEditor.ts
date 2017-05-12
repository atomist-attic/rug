import { Project } from "../model/Core";
import { PathExpressionEngine } from "../tree/PathExpression";
import { GitProjectLoader } from "./GitProjectLoader";

export interface ProjectContext {

  /**
   * Use to run path expressions against any node available in a project operation
   */
  pathExpressionEngine: PathExpressionEngine;

  /**
   * RepoResolver to use in loading repositories.
   */
  gitProjectLoader: GitProjectLoader;

  /**
   * Create an empty project
   */
  emptyProject();

}

export interface EditProject {

  /**
   * Edit the project given the parameters.
   */
  edit(project: Project, params?: {}): void;
}

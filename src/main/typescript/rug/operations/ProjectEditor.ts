import { Project } from "../model/Core";
import { PathExpressionEngine } from "../tree/PathExpression";
import { RugOperation } from "./RugOperation";

export interface ProjectContext {

  /**
   * Use to run path expressions against any node available in a project operation
   */
  pathExpressionEngine: PathExpressionEngine;
}

export interface EditProject {

  /**
   * Edit the project given the parameters.
   */
  edit(project: Project, params?: {}): void;
}

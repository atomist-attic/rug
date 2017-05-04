import { Project } from "../model/Core";
import { PathExpressionEngine } from "../tree/PathExpression";
import { RugOperation } from "./RugOperation";
import { MicrogrammarHelper } from "../tree/MicrogrammarHelper";

export interface ProjectContext {

  /**
   * Use to run path expressions against any node available in a project operation
   */
  pathExpressionEngine: PathExpressionEngine;

  microgrammarHelper: MicrogrammarHelper;
}

export interface EditProject {

  /**
   * Edit the project given the parameters.
   */
  edit(project: Project, params?: {}): void;
}

/**
 * Edits projects
 */
export interface ProjectEditor extends RugOperation, EditProject {

}

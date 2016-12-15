import {Project} from "../model/Core"
import {Parameter, Result, RugOperation} from "./RugOperation"
import {PathExpressionEngine} from "../tree/PathExpression"

export interface ProjectContext{
  pathExpressionEngine(): PathExpressionEngine
}

export interface ProjectEditor extends RugOperation{
  edit(project: Project, params: Object): Result
}

import {Project} from "../model/Core"
import {Parameter, Result, RugOperation} from "./RugOperation"

export interface ProjectEditor extends RugOperation{
  edit(project: Project, ...args: any[]): Result
}

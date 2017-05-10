import { Project } from "../model/Core";
import { RugOperation } from "./RugOperation";

export interface PopulateProject {
  populate(emptyProject: Project, params?: {});
}

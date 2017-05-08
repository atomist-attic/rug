import { Project } from "../model/Core";
import { RugOperation } from "./RugOperation";

interface PopulateProject {
  populate(emptyProject: Project, params?: {});
}
/**
 * Top level interface for all project generators
 */
interface ProjectGenerator extends RugOperation, PopulateProject {

}

export { ProjectGenerator, PopulateProject };

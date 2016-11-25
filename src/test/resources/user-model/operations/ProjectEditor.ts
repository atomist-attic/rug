import {Project} from '../model/Core'
import {Parameters} from "./Parameters"
import {ParametersSupport} from "./Parameters"

/**
 * Main entry point for project editing
 */
interface ProjectEditor<P extends Parameters> {

    edit(project: Project, p: P): string

}

export {ProjectEditor}

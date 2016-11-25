import {Project} from '../model/Core'
import {Parameters} from "./Parameters"
import {ParametersSupport} from "./Parameters"
import {Result} from "./Result"


/**
 * Main entry point for project editing
 */
interface ProjectEditor<P extends Parameters> {

    edit(project: Project, p: P): Result

}

export {ProjectEditor}

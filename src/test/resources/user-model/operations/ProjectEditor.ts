import {Project} from '../model/Core'
import {Parameters} from "./Parameters"
import {Result} from "./Result"

/**
 * Main entry point for project editing
 */
export interface ProjectEditor<P extends Parameters> {

    edit(project: Project, p: P): Result

}


/**
 * Convenience superclass for editors without parameters
 */
export abstract class ParameterlessProjectEditor implements ProjectEditor<Parameters> {

    edit(project: Project, p: Parameters) {
        return this.editWithoutParameters(project)
    }

    protected abstract editWithoutParameters(project: Project): Result
}

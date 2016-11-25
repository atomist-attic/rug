import {Project} from '../model/Core'
import {Parameters} from "./Parameters"

interface ProjectGenerator<P extends Parameters> {

    edit(project: Project, p: P): string

}

export {ProjectGenerator}

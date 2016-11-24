import {Project} from 'user-model/model/Core'

interface Parameters {
  // TODO should return a detailed failure? Or exception...
  validate(): boolean
}

abstract class ParametersSupport implements Parameters {

  validate() { return true; }
}


// TODO Nashorn doesn't seem to like exports
interface ProjectEditor<P extends Parameters> {

    edit(project: Project, p: P): string

}

export {Parameters}
export {ParametersSupport}
export {ProjectEditor}

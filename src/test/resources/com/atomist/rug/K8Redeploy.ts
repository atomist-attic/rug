import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'


interface Parameters {

    service: string
    new_sha: string

}

/**
       Update Kube spec to redeploy a service
*/
class Redeploy implements ProjectEditor {

    name: string = "Redeploy"
    
    description: string = "Update Kube spec to redeploy a service"
    
    parameters: Parameter[] = [{name: "service", pattern: "^[\\w.\\-_]+$", maxLength: -1, minLength: -1, validInput: "String value"}, {name: "new_sha", pattern: "^[a-f0-9]{7}$", maxLength: -1, minLength: -1, validInput: "String value"}];
    
    edit(project: Project, parameters: Parameters) {

         project.regexpReplace(parameters.service + ":[a-f0-9]{7}", parameters.service + ":" + parameters.new_sha)

    }

}

export const ked = new Redeploy()
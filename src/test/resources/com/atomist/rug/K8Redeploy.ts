import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

    
/**
       Update Kube spec to redeploy a service
*/
interface Parameters {

    service: string
    new_sha: string

}
     
class Redeploy implements ProjectEditor {

    name: string = "Redeploy"
    
    description: string = "Update Kube spec to redeploy a service"
    
    parameters: Parameter[] = [{name: "service", pattern: "^[\\w.\\-_]+$", maxLength: -1, minLength: -1, validInput: "String value"}, {name: "new_sha", pattern: "^[a-f0-9]{7}$", maxLength: -1, minLength: -1, validInput: "String value"}];
    
    edit(project: Project, parameters: Parameters) {
    
        let eng: PathExpressionEngine = project.context().pathExpressionEngine();
        
        let service = parameters.service
            let new_sha = parameters.new_sha
        
            let p = project
                if (true) {
                    p.regexpReplace((() => { 
                     return service + ":[a-f0-9]{7}"  })(),  service + ":" + new_sha )
                }
    
    }

}

export let ked = new Redeploy()
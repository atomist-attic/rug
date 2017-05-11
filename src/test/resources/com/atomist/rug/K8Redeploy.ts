import {Editor, Parameter} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'

/**
       Update Kube spec to redeploy a service
*/

@Editor("Update Kube spec to redeploy a service")
class Redeploy  {

    @Parameter({pattern: "^[\\w.\\-_]+$", maxLength: -1, minLength: -1, validInput: "String value"})
    service: string;

    @Parameter({pattern: "^[a-f0-9]{7}$", maxLength: -1, minLength: -1, validInput: "String value"})
    new_sha: string;

    edit(project: Project) {

         project.regexpReplace(this.service + ":[a-f0-9]{7}", this.service + ":" + this.new_sha)

    }

}

export const ked = new Redeploy()

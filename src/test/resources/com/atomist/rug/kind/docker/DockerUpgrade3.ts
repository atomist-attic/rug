
import {Editor} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'

import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

import {DockerFile} from '@atomist/rug/model/Core'

@Editor("DockerUpgrade")
class DockerUpgrade3  {

    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context.pathExpressionEngine;
        
        let exposePort = "8181"
    
            eng.with<DockerFile>(project, '//DockerFile()', d => {
                d.addOrUpdateExpose(exposePort)
                d.addOrUpdateFrom("java:8-jre")
            })
    
    }

}
export let editor_dockerUpgrade = new DockerUpgrade3();

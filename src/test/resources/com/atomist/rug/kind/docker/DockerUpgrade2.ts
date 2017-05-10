import {Editor} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'

import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

import {DockerFile} from '@atomist/rug/model/Core'

@Editor("DockerUpgrade")
class DockerUpgrade2  {

    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context.pathExpressionEngine;
        
        eng.with<DockerFile>(project, '//DockerFile()', d => {
            d.addOrUpdateExpose("8081")
            d.addOrUpdateFrom("java:8-jre")
            d.addOrUpdateHealthcheck("--interval=5s --timeout=3s CMD curl --fail http://localhost:8080/ || exit 1")
        })
    
    }

}
export let editor_dockerUpgrade = new DockerUpgrade2();

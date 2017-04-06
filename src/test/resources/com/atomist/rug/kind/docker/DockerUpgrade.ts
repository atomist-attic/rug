import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'

import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

import {DockerFile} from '@atomist/rug/model/Core'

class DockerUpgrade implements ProjectEditor {

    name: string = "DockerUpgrade"

    description: string = "DockerUpgrade"
    
    edit(project: Project) {

        let eng: PathExpressionEngine = project.context.pathExpressionEngine();

            eng.with<DockerFile>(project, '//DockerFile()', d => {
                d.addExpose("8081")
                d.addOrUpdateFrom("java:8-jre")
            })

    }

}
export let editor_dockerUpgrade = new DockerUpgrade();
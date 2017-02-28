import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Pom} from '@atomist/rug/model/Core'


class AddOrReplaceDependency implements ProjectEditor {

    name: string = "AddOrReplaceDependency"
    
    description: string = "AddOrReplaceDependency"
    
    edit(project: Project) {
        let eng: PathExpressionEngine = project.context().pathExpressionEngine();
        let p = project
            eng.with<Pom>(p, '//Pom()', o => {
                o.addOrReplaceDependency("mygroup", "myartifact")
            })
    }

}
export let editor_everyPomEdit = new AddOrReplaceDependency();
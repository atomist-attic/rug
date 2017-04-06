import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {EveryPom} from '@atomist/rug/model/Core'


class EveryPomEdit implements ProjectEditor {

    name: string = "EveryPomEdit"
    
    description: string = "EveryPomEdit"
    

    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context.pathExpressionEngine();
        
            let p = project
            eng.with<EveryPom>(p, '//EveryPom()', o => {
                o.setGroupId("mygroup")
            })

    }

}
export const editor_everyPomEdit = new EveryPomEdit();
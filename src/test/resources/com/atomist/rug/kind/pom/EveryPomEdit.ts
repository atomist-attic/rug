import {Editor} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {EveryPom} from '@atomist/rug/model/Core'

@Editor("EveryPomEdit")
class EveryPomEdit  {

    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context.pathExpressionEngine;
        
            let p = project
            eng.with<EveryPom>(p, '//EveryPom()', o => {
                o.setGroupId("mygroup")
            })

    }

}
export const editor_everyPomEdit = new EveryPomEdit();

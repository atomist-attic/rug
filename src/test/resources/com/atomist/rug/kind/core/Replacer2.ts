    
import {Editor} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'

import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

import {Replacer} from '@atomist/rug/model/Core'

@Editor("Replacer")
class Replacer2  {
    
    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context.pathExpressionEngine;
        
            project.replace("org.springframework", "nonsense")
    
            eng.with<Replacer>(project, '//Replacer()', Replacer => {
                 Replacer.replaceItNoGlobal("org.springframework", "nonsense")
            })
    
    }

}
export let editor_replacer = new Replacer2();

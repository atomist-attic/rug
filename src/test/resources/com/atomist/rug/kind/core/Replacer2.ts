    
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'

import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

import {Replacer} from '@atomist/rug/model/Core'

class Replacer2 implements ProjectEditor {

    name: string = "Replacer"
    
    description: string = "Replacer"
    
    
    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context().pathExpressionEngine();
        
            project.replace("org.springframework", "nonsense")
    
            eng.with<Replacer>(project, '//Replacer()', Replacer => {
                 Replacer.replaceItNoGlobal("org.springframework", "nonsense")
            })
    
    }

}
export let editor_replacer = new Replacer2();
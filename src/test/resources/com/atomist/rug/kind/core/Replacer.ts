import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Replacer} from '@atomist/rug/model/Core'

class Replacer1 implements ProjectEditor {

    name: string = "Replacer"
    
    description: string = "Replacer"
    
    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context().pathExpressionEngine();
    
            eng.with<Replacer>(project, '//Replacer()', r => {
                if (true) {
                    r.replaceIt("org.springframework", "nonsense")
                }
            })
    
    }
}
export let editor_replacer = new Replacer1();
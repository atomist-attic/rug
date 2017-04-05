import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {ReplacerClj} from '@atomist/rug/model/Core'

class Replacer implements ProjectEditor {

    name: string = "Replacer"
    
    description: string = "Replacer"
    
    edit(project: Project) {
        let eng: PathExpressionEngine = project.context.pathExpressionEngine();
        
        eng.with<ReplacerClj>(project, '//ReplacerClj()', r => {
            r.replaceIt("com.atomist.sample", "com.atomist.wassom")
        })
    
    }
}
export let editor_replacer = new Replacer();
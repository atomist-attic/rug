import {Editor} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {ReplacerClj} from '@atomist/rug/model/Core'

@Editor("Replacer")
class Replacer  {

    edit(project: Project) {
        let eng: PathExpressionEngine = project.context.pathExpressionEngine;
        
        eng.with<ReplacerClj>(project, '//ReplacerClj()', r => {
            r.replaceIt("com.atomist.sample", "com.atomist.wassom")
        })
    
    }
}
export let editor_replacer = new Replacer();

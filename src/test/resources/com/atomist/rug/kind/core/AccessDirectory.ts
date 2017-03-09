import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Directory} from '@atomist/rug/model/Core'

class AccessDirectory implements ProjectEditor {

    name: string = "AccessDirectory"
    
    description: string = "AccessDirectory"
    
    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context().pathExpressionEngine();
    
        let found = false
        eng.with<Directory>(project, '/src/main/java', dir => {
            found = true
            if (dir.totalFileCount() == 0)
                throw new Error("Should have reported files in the directory")
        })
        if (!found)
            throw new Error("Didn't find a directory")
    }
}
export let editor_replacer = new AccessDirectory();
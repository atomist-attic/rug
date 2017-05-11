import {Editor} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'
import {EventHandler, ResponseHandler, CommandHandler, Parameter, Tags, Intent} from "@atomist/rug/operations/Decorators";
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

/**
 * Generic edit, path in path expression
 */

@Editor("YamlEdit")
class YamlEdit  {

    @Parameter({description: "Path expression", pattern: "^.*$"})
    path: string

    @Parameter({description: "New value", pattern: "^.*$"})
    newValue: string
    
    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context.pathExpressionEngine;
        
            eng.with<any>(project, this.path, g => {
                    g.update(this.newValue )
            })
    }

}
export let editor_yamlEdit = new YamlEdit();

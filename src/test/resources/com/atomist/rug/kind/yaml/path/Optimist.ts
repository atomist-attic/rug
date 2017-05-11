import {Editor} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

@Editor("YamlEdit")
class YamlEdit  {

    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context.pathExpressionEngine;
        
        eng.with<any>(project, `/*[@name='x.yml']/YamlFile()/dependencies/*`, g => {
                g.update( g.value().replace("Death", "Life") )
        })
    
    }

}
export let editor_yamlEdit = new YamlEdit();

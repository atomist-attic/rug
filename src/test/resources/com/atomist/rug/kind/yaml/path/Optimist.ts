import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

class YamlEdit implements ProjectEditor {

    name: string = "YamlEdit"
    
    description: string = "YamlEdit"
    
    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context.pathExpressionEngine;
        
        eng.with<any>(project, `/*[@name='x.yml']/YamlFile()/dependencies/*`, g => {
                g.update( g.value().replace("Death", "Life") )
        })
    
    }

}
export let editor_yamlEdit = new YamlEdit();
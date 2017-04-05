import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Project, Xml} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'
import {PathExpression,PathExpressionEngine,TextTreeNode} from '@atomist/rug/tree/PathExpression'

class Xit implements ProjectEditor {

    name: string = "Xit"
    
    description: string = "Xit"
    
    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context.pathExpressionEngine();
        
        eng.with<TextTreeNode>(project, `/*[@name='pom.xml']/XmlFile()/project/groupId`, n => {
            n.update("<groupId>not-atomist</groupId>")
        })
    }
}

export let editor_xit = new Xit();

import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'

import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

import {SpringBootProject} from '@atomist/rug/model/Core'

class ClassAnnotated implements ProjectEditor {

    name: string = "ClassAnnotated"
    
    description: string = "I annotate classes"

    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context().pathExpressionEngine();
        
            eng.with<SpringBootProject>(project, '//SpringBootProject()', p => {
                if (true) {
                    p.annotateBootApplication("com.someone", "Foobar")
                }
            })
    
    }
}
export let editor_classAnnotated = new ClassAnnotated();
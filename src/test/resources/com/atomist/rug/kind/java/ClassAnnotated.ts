import {Editor} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'
import {Parameter} from '@atomist/rug/operations/RugOperation'

import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

import {SpringBootProject} from '@atomist/rug/model/Core'

@Editor("I annotate classes")
class ClassAnnotated  {

    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context.pathExpressionEngine;
        
        eng.with<SpringBootProject>(project, '//SpringBootProject()', p => {
            p.annotateBootApplication("com.someone", "Foobar")
        })
    
    }
}
export const editor_classAnnotated = new ClassAnnotated();

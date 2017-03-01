// Generated by Rug to TypeScript transpiler.

import { EditProject } from '@atomist/rug/operations/ProjectEditor'
import { PathExpressionEngine } from '@atomist/rug/tree/PathExpression'
import { Editor, Tags, Parameter } from '@atomist/rug/operations/Decorators'
import { Pattern } from '@atomist/rug/operations/RugOperation'
import { JavaType, Project } from '@atomist/rug/model/Core'

/**
    ClassAnnotated
    I move package
 */
@Editor("ClassAnnotated", "I move package")
class ClassAnnotated implements EditProject {

    edit(project: Project) {
        let eng: PathExpressionEngine = project.context().pathExpressionEngine()
        eng.with<JavaType>(project, '//JavaType()', c => {
            if (c.name() == "Dog") {
                c.movePackage("com.atomist")
            }
        })
    }
}
export let editor_classAnnotated = new ClassAnnotated();
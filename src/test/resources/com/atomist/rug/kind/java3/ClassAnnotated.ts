// Generated by Rug to TypeScript transpiler.

import { EditProject } from '@atomist/rug/operations/ProjectEditor'
import { PathExpressionEngine } from '@atomist/rug/tree/PathExpression'
import { Editor, Tags, Parameter } from '@atomist/rug/operations/Decorators'
import { Pattern } from '@atomist/rug/operations/RugOperation'
import { JavaType, Project } from '@atomist/rug/model/Core'

/**
    ClassAnnotated
    I add ExtendWith annotation
 */
@Editor("ClassAnnotated", "I add ExtendWith annotation")
class ClassAnnotated implements EditProject {

    edit(project: Project) {
        let eng: PathExpressionEngine = project.context().pathExpressionEngine()
        eng.with<JavaType>(project, '//JavaType()', c => {
            c.addAnnotation("org.junit.jupiter.api.extension", "ExtendWith(value = SpringExtension.class)")
        })
    }
}
export let editor_classAnnotated = new ClassAnnotated();
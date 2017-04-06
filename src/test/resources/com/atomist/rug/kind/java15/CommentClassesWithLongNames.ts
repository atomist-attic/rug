
import { EditProject } from '@atomist/rug/operations/ProjectEditor'
import { PathExpressionEngine } from '@atomist/rug/tree/PathExpression'
import { Editor, Tags, Parameter } from '@atomist/rug/operations/Decorators'
import { Pattern } from '@atomist/rug/operations/RugOperation'
import { JavaType, Project } from '@atomist/rug/model/Core'

@Editor("ClassAnnotated", "ClassAnnotated")
class ClassAnnotated implements EditProject {

    edit(project: Project) {
        let eng: PathExpressionEngine = project.context.pathExpressionEngine()
        eng.with<JavaType>(project, '//JavaType()', c => {
            let name: string = c.name
            if (name.length > 17) {
                c.setHeaderComment("It appears that Rod still likes long names")
            }
        })
    }
}
export let editor_classAnnotated = new ClassAnnotated();

import { EditProject } from '@atomist/rug/operations/ProjectEditor'
import { PathExpressionEngine } from '@atomist/rug/tree/PathExpression'
import { Editor, Tags, Parameter } from '@atomist/rug/operations/Decorators'
import { Pattern } from '@atomist/rug/operations/RugOperation'
import { JavaType, Project } from '@atomist/rug/model/Core'

@Editor("ClassExtended", "ClassExtended")
class ClassExtended implements EditProject {

    edit(project: Project) {
        console.log("Using path extended")
        let eng: PathExpressionEngine = project.context().pathExpressionEngine()
        eng.with<JavaType>(project, '//JavaType()', j => {
            if (j.inheritsFrom("NotRelevant")) {
                j.addAnnotation("com.foo", "Baz")
            }
        })
    }
}
export let editor_classExtended = new ClassExtended();
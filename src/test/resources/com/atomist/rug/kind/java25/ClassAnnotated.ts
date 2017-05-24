import { EditProject } from '@atomist/rug/operations/ProjectEditor'
import { PathExpressionEngine } from '@atomist/rug/tree/PathExpression'
import { Editor, Tags } from '@atomist/rug/operations/Decorators'
import { Pattern } from '@atomist/rug/operations/RugOperation'
import { JavaSource, JavaType, Project } from '@atomist/rug/model/Core'

@Editor("AddAnnotation", "Add an annotation to a JavaType")
@Tags("documentation")
export class AddAnnotation implements EditProject {

    edit(project: Project) {
        let eng: PathExpressionEngine = project.context.pathExpressionEngine;
        eng.with<JavaSource>(project, '//JavaSource()', j => {
            eng.with<JavaType>(j, '//JavaType()', c => {
                c.addAnnotation("com.test.annotations", "MyAnnotation");
                // c.removeAnnotation("com.test.annotations", "MyAnnotation");
            })
        })
    }
}

export const addAnnotation = new AddAnnotation();

import { EditProject } from '@atomist/rug/operations/ProjectEditor'
import { PathExpressionEngine } from '@atomist/rug/tree/PathExpression'
import { Editor, Tags, Parameter } from '@atomist/rug/operations/Decorators'
import { Pattern } from '@atomist/rug/operations/RugOperation'
import { JavaType, Project } from '@atomist/rug/model/Core'

@Editor("ClassAnnotated", "I add FooBar annotation")
class ClassAnnotated implements EditProject {

    edit(project: Project) {
        let eng: PathExpressionEngine = project.context.pathExpressionEngine()
        eng.with<JavaType>(project, '//JavaType()', c => {
            if(c.sourceFile.javaProject().javaFileCount() < 100) {
                c.addAnnotation("com.someone", "FooBar")
            }
        })
    }
}
export let editor_classAnnotated = new ClassAnnotated();
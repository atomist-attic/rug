import { Project, File } from '@atomist/rug/model/Core'
import { Editor } from '@atomist/rug/operations/Decorators'
import { PathExpressionEngine, Microgrammar } from '@atomist/rug/tree/PathExpression'

@Editor("SampleTypeScriptTest", "Uses Sample from TypeScript")
class SampleTypeScriptTest {

    edit(project: Project) {

        // or you can do this: let pom = project.findFile('pom.xml');

        let eng : PathExpressionEngine = project.context().pathExpressionEngine();

        eng.with<File>(project, `/File()[@name="pom.xml"]`, pom => {
            pom.replace('dependency', 'dependenciesAreForBirds');
        })

    }
}
export let editor = new SampleTypeScriptTest();
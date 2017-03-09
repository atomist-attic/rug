import { Project } from '@atomist/rug/model/Core'
import { Editor } from '@atomist/rug/operations/Decorators'

@Editor("SampleTypeScriptTest", "Uses Sample from TypeScript")
class SampleTypeScriptTest {

    edit(project: Project) {

        let pom = project.findFile('pom.xml');

        pom.replace('dependency', 'dependenciesAreForBirds');

    }
}
export let editor = new SampleTypeScriptTest();
import { Project } from '@atomist/rug/model/Core'
import { Editor } from '@atomist/rug/operations/Decorators'

@Editor("MatcherMicrogrammarConstructionTypeScriptTest", "Uses MatcherMicrogrammarConstruction from TypeScript")
class MatcherMicrogrammarConstructionTypeScriptTest {

    edit(project: Project) {

        let pom = project.findFile('pom.xml');

        pom.replace('dependency', 'dependenciesAreForBirds');

    }
}
export let editor = new MatcherMicrogrammarConstructionTypeScriptTest();
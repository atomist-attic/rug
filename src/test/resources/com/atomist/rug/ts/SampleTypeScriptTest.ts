import {Project} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TreeNode,Microgrammar} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/RugOperation'

class SampleTypeScriptTest implements ProjectEditor {
    name: string = "Constructed";
    description: string = "Uses single microgrammar";

    edit(project: Project) {

        let pom = project.findFile('pom.xml');

        pom.replace('dependency', 'dependenciesAreForBirds');

    }
}
export let editor = new SampleTypeScriptTest();
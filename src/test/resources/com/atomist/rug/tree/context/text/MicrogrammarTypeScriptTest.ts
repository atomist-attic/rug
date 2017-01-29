import {Project} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Editor} from '@atomist/rug/operations/Decorators'
import {PathExpression,TreeNode,Microgrammar} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'


@Editor("MicrogrammarTypeScriptTest", "Uses Microgrammar from TypeScript")
class MicrogrammarTypeScriptTest {

    edit(project: Project) {

        let mg = new Microgrammar('oldObject', 'object $oldObjectName:§TheTestWillChangeThis§ {');
        let eng: PathExpressionEngine = project.context().pathExpressionEngine().addType(mg)

        eng.with<any>(project, "//File()/oldObject()/oldObjectName", n => {
            console.log("hello")
            if (n === null) {
                console.log("I think it is null")
            } else {
                console.log("I wish I could print this " + n.value());
            }
            n.update("TheTestHasChangedThis")
        })

    }
}
export let editor = new MicrogrammarTypeScriptTest();
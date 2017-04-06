import { Project, File } from '@atomist/rug/model/Core'
import { Editor } from '@atomist/rug/operations/Decorators'
import { PathExpressionEngine, Microgrammar } from '@atomist/rug/tree/PathExpression'

@Editor("MultipleUseOfSubmatcherTypeScriptTest", "Uses MultipleUseOfSubmatcher from TypeScript")
class MultipleUseOfSubmatcherTypeScriptTest {

    edit(project: Project) {

        let mg = new Microgrammar('things', 'I like $something, $something, $something, and $something.',
            { something: '§[a-z]+§'});

        let eng : PathExpressionEngine = project.context.pathExpressionEngine.addType(mg);

        eng.with<any>(project, `/File()[@name="targetFile"]/things()`, things => {

            let somethings = things.something();
            // console.log(`the whole thing is ${things} and the somethings are ${somethings}`);

            somethings[0].update("(1) " + somethings[0].value());
            somethings[1].update("(2) " + somethings[1].value());
            somethings[2].update("(3) " + somethings[2].value());
            somethings[3].update("(4) " + somethings[3].value());
        })

    }
}
export const editor = new MultipleUseOfSubmatcherTypeScriptTest();

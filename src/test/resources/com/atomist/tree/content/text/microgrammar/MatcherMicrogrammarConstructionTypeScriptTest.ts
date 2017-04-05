import { Project } from '@atomist/rug/model/Core'
import { Editor } from '@atomist/rug/operations/Decorators'
import { Microgrammar, TextTreeNode } from '@atomist/rug/tree/PathExpression'
import { Or } from '@atomist/rug/tree/Microgrammars'


@Editor("MatcherMicrogrammarConstructionTypeScriptTest", "Uses MatcherMicrogrammarConstruction from TypeScript")
class MatcherMicrogrammarConstructionTypeScriptTest {

    edit(project: Project) {

        let mg = new Microgrammar("testMe", `I like $vegetable`,
            { vegetable: Or(["broccoli", "carrots"]) });

        let eng = project.context.pathExpressionEngine().addType(mg);

        eng.with<TextTreeNode>(project, "/targetFile/testMe()", e => {
            //console.log("Found " + e.value())
            e.update(e.value() + " (which is a vegetable)")
        })

    }
}
export let editor = new MatcherMicrogrammarConstructionTypeScriptTest();
import { Project } from '@atomist/rug/model/Core'
import { Editor } from '@atomist/rug/operations/Decorators'
import { Microgrammar, TextTreeNode } from '@atomist/rug/tree/PathExpression'
import { Or, Optional, Regex, Repeat } from '@atomist/rug/tree/Microgrammars'


@Editor("MatcherMicrogrammarConstructionTypeScriptTest", "Uses MatcherMicrogrammarConstruction from TypeScript")
class MatcherMicrogrammarConstructionTypeScriptTest {

    edit(project: Project) {

        let mg = new Microgrammar("testMe", `public $returnType $functionName($params)`, {
            returnType: Or(["Banana", "Fruit"]),
            functionName: "$javaIdentifier",
            params: Repeat("$param"),
            param: "$javaType $javaIdentifier $comma",
            comma: Optional(","),
            javaType: Regex("[A-Za-z0-9_]+"),
            javaIdentifier: Regex("[a-zA-Z0-9]+")
        });

        let eng = project.context.pathExpressionEngine.addType(mg);

        let shouldMatch = project.findFile("targetFile").content;
        console.log("should match:" + shouldMatch);
        let result: any = project.context.microgrammarHelper.strictMatch(mg, shouldMatch);
        console.log("Result:" + result);


        eng.with<any>(project, "/targetFile/testMe()", e => {

            console.log("Found " + e.returnType.value())
            e.returnType.update("Fruit");
        })

    }
}
export let editor = new MatcherMicrogrammarConstructionTypeScriptTest();
import { Project } from '@atomist/rug/model/Core'
import { Editor } from '@atomist/rug/operations/Decorators'
import { Microgrammar, TextTreeNode } from '@atomist/rug/tree/PathExpression'
import { Or, Optional, Regex, Repeat } from '@atomist/rug/tree/Microgrammars'


@Editor("MatcherMicrogrammarConstructionTypeScriptTest", "Uses MatcherMicrogrammarConstruction from TypeScript")
class MatcherMicrogrammarConstructionTypeScriptTest {

    edit(project: Project) {

        let mg = new Microgrammar("testMe", `public $returnType $typeParameters $functionName($params)`, {
            returnType: Or(["Banana", "Fruit"]),
            typeParameters: Optional("<$typeVariable>"),
            typeVariable: Regex("[A-Z]\w*"),
            functionName: "$javaIdentifier",
            params: Repeat("$param"),
            param: "$javaType $javaIdentifier $comma",
            comma: Optional(","),
            javaType: Regex("[A-Za-z0-9_]_+"),
            javaIdentifier: Regex("[a-z]\w*")
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
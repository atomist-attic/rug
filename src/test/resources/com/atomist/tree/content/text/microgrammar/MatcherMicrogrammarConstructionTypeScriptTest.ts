import { Project, File } from '@atomist/rug/model/Core'
import { Editor } from '@atomist/rug/operations/Decorators'
import { Microgrammar, TextTreeNode } from '@atomist/rug/tree/PathExpression'
import { Or, Optional, Regex, Repeat, Literal, Concat } from '@atomist/rug/tree/Microgrammars'


@Editor("MatcherMicrogrammarConstructionTypeScriptTest", "Uses MatcherMicrogrammarConstruction from TypeScript")
class MatcherMicrogrammarConstructionTypeScriptTest {

    edit(project: Project) {

        let mg = new Microgrammar("testMe", `public $returnType $functionName($params)$pickyFormattedReturn`, {
            returnType: Or(["Banana", "Fruit"]),
            functionName: "$javaIdentifier",
            params: Repeat("$param"),
            param: "$javaType $javaIdentifier $comma",
            comma: Optional(", "),
            javaType: Regex("[A-Za-z0-9_]+"),
            javaIdentifier: Regex("[a-zA-Z0-9]+"),
            pickyFormattedReturn: Concat([Literal(": "), Regex("[A-Za-z0-9_]+")])
        });

        let eng = project.context.pathExpressionEngine.addType(mg);

        let shouldMatch = project.findFile("targetFile").content;
        console.log("should match:" + shouldMatch);
        let result: any = project.context.microgrammarHelper.strictMatch(mg, shouldMatch);
        // I don't think instanceof is right here, I think the operator for built-in types is different
        if (result instanceof String) { console.log("Result:" + result); }

        eng.with<any>(project, "/targetFile/testMe()", e => {

            console.log("Found " + e.returnType.value())
            e.returnType.update("Fruit");

            e.functionName.update("grow")

            e.params.update("int qty")
        })

        let exists = false
        eng.with<any>(project, "/unmatchingFile", e => {
            exists = true;
        })
        if (!exists) {
            throw "the unmatching file needs to be here so I can be sure it didn't match"
        }

        eng.with<any>(project, "/unmatchingFile/testMe()", e => {
            throw "That should not have matched"
        })

    }
}
export let editor = new MatcherMicrogrammarConstructionTypeScriptTest();
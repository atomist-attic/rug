import { Project } from '@atomist/rug/model/Core'
import { PathExpressionEngine, Microgrammar } from '@atomist/rug/tree/PathExpression'
import { Editor } from '@atomist/rug/operations/Decorators'
import { Regex, SkipAhead, Literal } from '@atomist/rug/tree/Microgrammars'

/*
 * This is a single-use editor
 * Delete it when you're done looking at it, or modify it for the next single use
 *
 * It pulls all the triple-quoted strings out of a Scala test and puts them in rug files.
 * Then I can use a different editor (in a different branch of rug) to turn them into typescript.
 */

@Editor("UpgradeRegexParsing", "Can I use new microgrammar functionality to upgrade old mg functionality??")
class UpgradeRegexParsing {

    edit(project: Project) {

        let mg = new Microgrammar("microgrammarMicrogrammar",
            `Microgrammar ( "$mgName" , \` $interestingBit $endString $endOfMG`,
            {
                variableStart: Literal("$"),
                mgName: Regex("[a-zA-Z_0-9-]+"),
                interestingBit: SkipAhead(` $variableStart $subName $embeddedRegex `),
                embeddedRegex: ":§ $regexContent §",
                subName: Regex("[a-zA-Z_0-9-]+"),
                regexContent: Regex("[^§]*"),
                endOfMG: ")",
                endString: SkipAhead("`")
            });

        let eng = project.context.pathExpressionEngine.addType(mg);

        let editMe = '/src/test/scala/com/atomist/rug//microgrammarMicrogrammar()';

        console.log("doing something");

        let count = 1;

        eng.with<any>(project, editMe, e => {
            console.log("Found one: " + e)
            e.endOfMg.update(` { ${e.subName.value()}: Regex(${e.regexContent.value()}) } )`)
            e.embeddedRegex.update('');
        });


    }
}
export let editor = new UpgradeRegexParsing();
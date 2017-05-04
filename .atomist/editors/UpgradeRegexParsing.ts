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
            `Microgrammar ( $mgName , \` $interestingBit $endString $endOfMG`,
            {
                variableStart: Literal("$"),
                mgName: Regex(`["'][a-zA-Z_0-9-]+["']`),
                interestingBit: SkipAhead(` $variableStart $subName $embeddedRegex `),
                embeddedRegex: ":§ $regexContent §",
                subName: Regex("[a-zA-Z_0-9-]+"),
                regexContent: Regex("[^§]*"),
                endOfMG: "$closeParen",
                closeParen: ")",
                endString: SkipAhead("`")
            });

        // TEST THE MICROGRAMMAR
        let shouldMatch = `Microgrammar('modelVersion', \` < modelVersion > $version:§[a-zA - Z0 - 9_\\.]+§</modelVersion>\`)`
        console.log("should match:" + shouldMatch);
        let result: any = project.context.microgrammarHelper.strictMatch(mg, shouldMatch);
        // I don't think instanceof is right here; how do I ask, is it a string?
        console.log("Result:" + result);

        // DO THE THINGS

        let eng = project.context.pathExpressionEngine.addType(mg);

        let editMe = '/src/test/scala/com/atomist//microgrammarMicrogrammar()';

        console.log("looking in: " + editMe);

        let count = 1;

        eng.with<any>(project, editMe, e => {
            console.log("Found one: " + e)
            let submatcherName = e.interestingBit.subName.value();
            let regexContent = e.interestingBit.embeddedRegex.regexContent.value()
            e.endOfMG.update(` { ${submatcherName}: Regex(${regexContent}) } )`)
            e.interestingBit.embeddedRegex.update('');
        });


    }
}
export let editor = new UpgradeRegexParsing();
import {Project} from '@atomist/rug/model/Core'
import { PathExpressionEngine , Microgrammar } from '@atomist/rug/tree/PathExpression'
import {Editor} from '@atomist/rug/operations/Decorators'

/*
 * This is a single-use editor
 * Delete it when you're done looking at it, or modify it for the next single use
 *
 * It pulls all the triple-quoted strings out of a Scala test and puts them in rug files.
 * Then I can use a different editor (in a different branch of rug) to turn them into typescript.
 */

@Editor("JavaTypeUsageTestTypeScriptTest", "Uses JavaTypeUsageTest from TypeScript")
class JavaTypeUsageTestTypeScriptTest {

    edit(project: Project) {

        let mg = new Microgrammar("tripleQuotedEditor", `§s{0,1}§"""¡"""¡.stripMargin`,
            {});

        let eng = project.context.pathExpressionEngine.addType(mg);

        let editors = '/src/test/scala/com/atomist/rug/kind/java/*[@name="JavaTypeUsageTest.scala"]/tripleQuotedEditor()';

        console.log("doing something");

        let count = 1;

        eng.with<any>(project, editors, e => {
            console.log(`Found editor: ${e.value()}`);
            let quotesBegin = e.value().indexOf(`"""`);
            let content = e.value().substr(quotesBegin + 3, e.value().length - (18 + quotesBegin)); // strip """

            if (content.indexOf("|editor") > 0) {

                // my javascript is terrible
                let lines = content.split("\n");
                let newLines = [];
                let editorName = null;
                lines.forEach(f => {
                    let noPipe = f.replace(/^.*\|/, "");
                    if (/^editor /.test(noPipe)) {
                        editorName = noPipe.substr(7)
                    }
                    newLines = newLines.concat([noPipe])
                })

                let fileName = "com/atomist/rug/kind/java" + count + "/" + editorName + ".rug";
                project.addFile("src/test/resources/" + fileName, newLines.join("\n"))
                count++;

                e.update(`ClassPathArtifactSource.toArtifactSource("${fileName}").withPathAbove(".atomist/editors")`)

            }

        });


    }
}
export let editor = new JavaTypeUsageTestTypeScriptTest();
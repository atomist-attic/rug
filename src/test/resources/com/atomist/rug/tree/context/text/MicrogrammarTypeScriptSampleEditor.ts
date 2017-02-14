import {Project} from '@atomist/rug/model/Core'
import {Editor} from '@atomist/rug/operations/Decorators'
import {Microgrammar} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'


/* I used this to refactor a test.
 * I had calls that look like:
 * val Right(output) = mg.strictMatch("input stuff")
 *
 * and I need to replace strictMatch with a utility function call that does more
 *
 * val output = strictMatchAndFakeAFile(mg, "input stuff")
 *
 * so I made an editor that does this. So here it is.
 *
 */
@Editor("MicrogrammarTypeScriptSampleEditor", "Uses Microgrammar from TypeScript")
class MicrogrammarTypeScriptSampleEditor {

    edit(project: Project) {

        function regex(s: String) { return `§${s}§` }

        let lowercaseIdentifier = regex('[a-z][A-Za-z_]*');
        let javaString = regex('"[^"]*"');

        let mg = new Microgrammar('strictMatchCall',
            'val Right($outputVar) = $mg.strictMatch($inputString)',
            { outputVar: lowercaseIdentifier,
              mg: lowercaseIdentifier,
              inputString: javaString } ); // for this to be useful I need to say javaExpression here
        let eng: PathExpressionEngine = project.context().pathExpressionEngine().addType(mg)

        eng.with<any>(project, `//File()/strictMatchCall()`, n => {
            n.update(`val ${n.outputVar().value()} = strictMatchAndFakeAFile(${n.mg().value()}, ${n.inputString().value()})`)
        })

    }
}
export let editor = new MicrogrammarTypeScriptSampleEditor();
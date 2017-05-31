import {Project, File} from '@atomist/rug/model/Core'
import {Editor} from '@atomist/rug/operations/Decorators'
import {PathExpressionEngine, TextTreeNode} from '@atomist/rug/tree/PathExpression'
import {Parameter} from "@atomist/rug/operations/Decorators";
import {Pattern} from "@atomist/rug/operations/RugOperation";

@Editor("PandaParsingTypeScriptTest", "Uses PandaParsing from TypeScript")
class PandaParsingTypeScriptTest {

    @Parameter({pattern: Pattern.any})
    changePandasInCurliesTo: string;

    @Parameter({pattern: Pattern.any})
    changeFirstPandaInEachLineTo: string;

    edit(project: Project) {

        let eng: PathExpressionEngine = project.context.pathExpressionEngine;

        let pandafile = eng.scalar<Project, File>(project, `/my.panda`);
        let parsedPanda = eng.scalar<File, TextTreeNode>(pandafile, "/Panda()");

        eng.with<TextTreeNode>(parsedPanda, `//curly/word`, pandaWord => {
            pandaWord.update(this.changePandasInCurliesTo);
        });

        eng.with<TextTreeNode>(parsedPanda, `/line/word[1]`, pandaWord => {
            //console.log("Found first word in line: " + (pandaWord.parent() as TextTreeNode).value());
            pandaWord.update(this.changeFirstPandaInEachLineTo);
        })

    }
}

export let editor = new PandaParsingTypeScriptTest();

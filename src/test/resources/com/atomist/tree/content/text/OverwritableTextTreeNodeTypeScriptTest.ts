import {Project} from '@atomist/rug/model/Core'
import {Editor} from '@atomist/rug/operations/Decorators'
import {Microgrammar, PathExpressionEngine, PathExpression, TextTreeNode} from '@atomist/rug/tree/PathExpression'
import {Regex} from '@atomist/rug/tree/Microgrammars'

/*
 * Used in com/atomist/tree/content/text/OverwritableTextTreeNodeChild.scala
 */
@Editor("OverwritableTextTreeNodeTypeScriptTest", "Uses OverwritableTextTreeNode from TypeScript")
class OverwritableTextTreeNodeTypeScriptTest {

    edit(project: Project) {

        let pom = project.findFile('pom.xml');

        let mg = new Microgrammar('parent',
            `
<parent>
<groupId>$groupId</groupId>
<artifactId>$artifactId</artifactId>
<version>$version</version>
$whateverElse
</parent>
`,
            { groupId: Regex('[a-z.]+'),
              artifactId: Regex('[a-z-]+'),
              version: Regex('[0-9]+\.[0-9]+\.[0-9]+[.A-Za-z-_]*'),
              whateverElse: Regex('.*')
            }
          );


        let eng: PathExpressionEngine = project.context.pathExpressionEngine.addType(mg)

        let mgNode = `/File()[@name="pom.xml"]/parent()`

        eng.with<any>(project, mgNode, parent => {
            //console.log(`I matched: ${parent}`);
            let versionNumber = parent.version;
            parent.update("Everything under me is now invalidated! wahaha!");
            versionNumber.update("This should fail!!")
        })

        pom.replace('dependency', 'dependenciesAreForBirds');
    }
}

export const editor = new OverwritableTextTreeNodeTypeScriptTest();
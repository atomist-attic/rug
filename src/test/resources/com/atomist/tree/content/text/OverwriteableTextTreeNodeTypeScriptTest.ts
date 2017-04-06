import {Project} from '@atomist/rug/model/Core'
import {Editor} from '@atomist/rug/operations/Decorators'
import {Microgrammar, PathExpressionEngine, PathExpression, TextTreeNode} from '@atomist/rug/tree/PathExpression'

/*
 * Used in com/atomist/tree/content/text/OverwritableTextTreeNodeChild.scala
 */
@Editor("OverwriteableTextTreeNodeTypeScriptTest", "Uses OverwriteableTextTreeNode from TypeScript")
class OverwriteableTextTreeNodeTypeScriptTest {

    edit(project: Project) {

        let pom = project.findFile('pom.xml');

        let regex = function(rex: string) {
            return `§${rex}§`
        }

        let mg = new Microgrammar('parent',
            `
<parent>
<groupId>$groupId</groupId>
<artifactId>$artifactId</artifactId>
<version>$version</version>
$whateverElse
</parent>
`,
            { groupId: regex('[a-z.]+'),
              artifactId: regex('[a-z-]+'),
              version: regex('[0-9]+\.[0-9]+\.[0-9]+[.A-Za-z-_]*'),
              whateverElse: regex('.*')
            }
          );


        let eng: PathExpressionEngine = project.context.pathExpressionEngine.addType(mg)


        let mgNode = `/File()[@name="pom.xml"]/parent()`

        eng.with<any>(project, mgNode, parent => {
            //console.log(`I matched: ${parent}`);
            let versionNumber = parent.version();
            parent.update("Everything under me is now invalidated! wahaha!");
            versionNumber.update("This should fail!!")
        })

        pom.replace('dependency', 'dependenciesAreForBirds');

    }
}
export let editor = new OverwriteableTextTreeNodeTypeScriptTest();
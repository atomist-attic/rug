import {Project} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TreeNode,Microgrammar} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'

class ExtractTypescriptConstants implements ProjectEditor {
    name: string = "ExtractTypescriptConstants"
    description: string = "Uses single microgrammar"

    edit(project: Project) {
      // `"""$stringContents:§.*§"""`
        let tripleQuotedStringMG = new Microgrammar('tripleQuotedString', `package`)
        let eng: PathExpressionEngine = project.context().pathExpressionEngine().addType(tripleQuotedStringMG)

        // "/File()/tripleQuotedString()/stringContents()"
        eng.with<TreeNode>(project, "/*/tripleQuotedString()", n => {
            console.log(n.nodeName())
        })
    }
}
var editor = new ExtractTypescriptConstants()

import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

/**
 * Removes double spacing from Scala and Java files.
 */
class RemoveDoubleSpacedLines implements ProjectEditor {
    name: string = "RemoveDoubleSpacedLines"
    description: string = "Remove double spacing"

    edit(project: Project) {
        let eng: PathExpressionEngine = project.context().pathExpressionEngine()

        eng.with<File>(project, `/src//File()`, f => {
            // console.log(`The file is ${f}`)
            f.regexpReplace("\n\n$", "\n")
        })
    }
}

export let editor = new RemoveDoubleSpacedLines()
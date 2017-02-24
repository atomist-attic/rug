import {Project} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/RugOperation'


class DeliberateNpe implements ProjectEditor {
    name: string = "DeliberateNpe"
    description: string = "deliberate npe"

    edit(project: Project) {
        let x = null
        x.throwNpe()
    }
  }

export let editor = new DeliberateNpe();
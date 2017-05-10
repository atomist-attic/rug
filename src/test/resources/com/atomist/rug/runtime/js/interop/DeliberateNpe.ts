import {Project} from '@atomist/rug/model/Core'
import {Editor} from '@atomist/rug/operations/Decorators'
import {PathExpression,TreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/RugOperation'


@Editor("deliberate npe")
class DeliberateNpe{
    edit(project: Project) {
        let x = null
        x.throwNpe()
    }
  }

export let editor = new DeliberateNpe();

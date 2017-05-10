import {Editor} from "@atomist/rug/operations/Decorators"
import {Project,Pair} from '@atomist/rug/model/Core'
import {Match,PathExpression,PathExpressionEngine,TreeNode} from '@atomist/rug/tree/PathExpression'

@Editor("rename", "rename")
class Rename {
    edit(project: Project) {
      let eng: PathExpressionEngine = project.context.pathExpressionEngine;
      eng.with<Pair>(project, "/*[@name='package.json']/Json()/subdomain", d => {
        d.setValue ("absquatulate")
      })
    }
}

export const finder = new Rename();

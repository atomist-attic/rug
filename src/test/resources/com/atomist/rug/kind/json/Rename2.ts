import {Editor} from "@atomist/rug/operations/Decorators"
import {Status, Result} from "@atomist/rug/operations/RugOperation"
import {Project,Pair} from '@atomist/rug/model/Core'
import {Match,PathExpression,PathExpressionEngine,TreeNode} from '@atomist/rug/tree/PathExpression'

@Editor("Rename", "Rename")
class Rename2 {

    edit(project: Project) {

      let eng: PathExpressionEngine = project.context.pathExpressionEngine;
      eng.with<Pair>(project, `/*[@name='package.json']/Json()/dependencies`, p =>
       p.addKeyValue("foo", "bar")
     )
    }
}

export let finder = new Rename2();

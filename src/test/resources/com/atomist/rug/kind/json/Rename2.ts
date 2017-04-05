import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
import {Status, Result} from "@atomist/rug/operations/RugOperation"
import {Project,Pair} from '@atomist/rug/model/Core'
import {Match,PathExpression,PathExpressionEngine,TreeNode} from '@atomist/rug/tree/PathExpression'

class Rename2 implements ProjectEditor {
    name: string = "Rename"
    description: string = "Rename"

    edit(project: Project) {

      let eng: PathExpressionEngine = project.context.pathExpressionEngine();
      eng.with<Pair>(project, `/*[@name='package.json']/Json()/dependencies`, p =>
       p.addKeyValue("foo", "bar")
     )
    }
}

export let finder = new Rename2();
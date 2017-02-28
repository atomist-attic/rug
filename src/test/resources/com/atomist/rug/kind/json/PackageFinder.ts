import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
import {Status, Result} from "@atomist/rug/operations/RugOperation"
import {Project,Pair} from '@atomist/rug/model/Core'
import {Match,PathExpression,PathExpressionEngine,TreeNode} from '@atomist/rug/tree/PathExpression'

class PackageFinder implements ProjectEditor {
    name: string = "node.deps"
    description: string = "Finds package.json dependencies"
    edit(project: Project) {

      let eng: PathExpressionEngine = project.context().pathExpressionEngine();
      let pe = new PathExpression<Project,Pair>(`/*[@name='package.json']/Json()/dependencies`)
      let p = eng.scalar(project, pe)
      //if (p == null)
      p.addKeyValue("foo", "bar")
    }
}

export let finder = new PackageFinder();
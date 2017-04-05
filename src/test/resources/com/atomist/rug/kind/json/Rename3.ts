import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
import {Project,SpringBootProject,Pair} from '@atomist/rug/model/Core'
import {Match,PathExpression,PathExpressionEngine,TreeNode} from '@atomist/rug/tree/PathExpression'

class Rename3 implements ProjectEditor {
    name: string = "rename"
    description: string = "Rename"

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context.pathExpressionEngine();
      project.files.forEach(f => {
          if (f.name == "package.json")
        eng.with<Pair>(f, "/Json()/subdomain", d => {
            d.setValue ("absquatulate")
        }) 
    })
    }
}

export let finder = new Rename3()
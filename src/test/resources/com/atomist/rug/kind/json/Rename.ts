import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
import {Project,SpringBootProject,Pair} from '@atomist/rug/model/Core'
import {Match,PathExpression,PathExpressionEngine,TreeNode} from '@atomist/rug/tree/PathExpression'

class Rename implements ProjectEditor {
    name: string = "rename"
    description: string = "Rename"

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context.pathExpressionEngine;
      eng.with<Pair>(project, "/*[@name='package.json']/Json()/subdomain", d => {
        d.setValue ("absquatulate")
      })
    }
}

export let finder = new Rename()
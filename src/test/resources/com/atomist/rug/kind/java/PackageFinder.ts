import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
import {Project,SpringBootProject} from '@atomist/rug/model/Core'
import {Match,PathExpression,PathExpressionEngine,TreeNode} from '@atomist/rug/tree/PathExpression'

class PackageFinder implements ProjectEditor {
    name: string = "package.finder"
    description: string = "Find a spring boot package"
    edit(project: Project) {
      let eng: PathExpressionEngine = project.context.pathExpressionEngine;
      let pe = new PathExpression<Project,SpringBootProject>("/SpringBootProject()")
      let p = eng.scalar(project, pe)
    }
}

export let finder = new PackageFinder()
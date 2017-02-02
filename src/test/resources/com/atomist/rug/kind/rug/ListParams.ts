import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Status, Result, Parameter} from '@atomist/rug/operations/RugOperation'

class Editors implements ProjectEditor {
    name: string = "Editors"
    description: string = "Uses antlr to parse Rug DSL and list params"

    edit(project: Project) {
        let eng: PathExpressionEngine = project.context().pathExpressionEngine()

        let count = 0
        eng.with<TreeNode>(project, "//RugFile()/rug/param", n => {
            let pe = new PathExpression<TreeNode, TreeNode>("/param_name")
            let param_name = eng.scalar(n, pe)

            pe = new PathExpression<TreeNode, TreeNode>("/param_value")
            let param_value = eng.scalar(n, pe)

            count++
          })

          if (count == 0)
           throw new Error("No params found. Sad.")
        }
  }
export let editor = new Editors();
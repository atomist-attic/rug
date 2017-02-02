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
        eng.with<TreeNode>(project, "//RugFile()/rug[/param/param_value[@value='@semantic_version']]/name", n => {
            count++
          })

          if (count == 0)
           throw new Error("No editor using semver found. Sad.")
        }
  }
export let editor = new Editors();
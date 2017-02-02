import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/RugOperation'

class Editors implements ProjectEditor {
    name: string = "Editors"
    description: string = "Uses antlr to parse Rug DSL"

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context().pathExpressionEngine()

    let count = 0
    eng.with<TreeNode>(project, "//RugFile()/annotation[/annotation_name[@value='@generator']]", n => {
        let pe = new PathExpression<TreeNode, TreeNode>("/annotation_value")
        let gen_name = eng.scalar(n, pe).value().replace(/"/g, '')
        n.update("")

        eng.with<TreeNode>(project, `//RugFile()/rug[/name[@value='${gen_name}']]/type`, n => {
            n.update("generator")
            count++
        })
      })

      if (count == 0)
       throw new Error("No '@generator' annotation found and replaced. Sad.")
    }
  }
export let editor = new Editors();
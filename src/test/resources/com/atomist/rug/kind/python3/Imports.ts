import {Project} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/RugOperation'

class Imports implements ProjectEditor {
    name: string = "Constructed"
    description: string = "Uses single microgrammar"

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context().pathExpressionEngine()

      let i = 0
      eng.with<TreeNode>(project, "//File()/PythonRawFile()//import_stmt()", n => {
        console.log(`The node is ${n.value()}`)
        i++
      })
      if (i == 0)
       throw new Error("No Pythons found. Sad.")
    }
  }
export let editor = new Imports();
import {Project,File} from '@atomist/rug/model/Core'
import {Editor} from '@atomist/rug/operations/Decorators'
import {PathExpression,TreeNode,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/RugOperation'

@Editor("Imports")
class Imports  {

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context.pathExpressionEngine

    eng.with<TextTreeNode>(project, "//File()/PythonFile()//import_from()//dotted_name", n => {
        //console.log(`The FROM value is '${n.value()}'`)
      })

      let count = 0
      eng.with<File>(project, "//File()[/PythonFile()//import_from()//dotted_name[@value='flask']]", n => {
        //console.log(`The file path is '${n.path()}'`)
        count++
      })

      if (count == 0)
       throw new Error("No files with flask imports found. Sad.")
    }
  }
export let editor = new Imports();

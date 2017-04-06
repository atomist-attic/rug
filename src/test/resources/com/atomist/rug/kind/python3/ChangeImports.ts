import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/RugOperation'

class ChangeImports implements ProjectEditor {
    name: string = "Constructed"
    description: string = "Uses single microgrammar"

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context.pathExpressionEngine()

      let count = 0
      eng.with<TextTreeNode>(project, "//File()/PythonFile()//import_from()//dotted_name[@value='flask']", n => {
        //console.log(`The import was '${n.value()}'`)
        n.update("newImport")
        count++
      })

      if (count == 0)
       throw new Error("No files with flask imports found. Sad.")
    }
  }
export let editor = new ChangeImports();
import {Project,File} from '@atomist/rug/model/Core'
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


      let count = 0
      eng.with<File>(project, "//File()[/CSharpFile()//using_directive]", n => {
        console.log(`The file path is '${n.path()}'`)
        count++
      })

      if (count == 0)
       throw new Error("No C# files with imports found. Sad.")
    }
  }

export let editor = new Imports();
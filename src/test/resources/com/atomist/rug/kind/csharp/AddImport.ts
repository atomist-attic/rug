import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/RugOperation'

class AddImport implements ProjectEditor {
    name: string = "Constructed"
    description: string = "Uses single microgrammar"

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context().pathExpressionEngine()

      let count = 0
      eng.with<TextTreeNode>(project, "//File()/CSharpFile()//using_directive[1]", n => {
        //console.log(`The using was '${n.value()}'`)
        n.update(n.value() + "\nusing System.Linq;")
        count++
      })

      if (count == 0)
       throw new Error("No C# files using directive found. Sad.")
    }
}

export let editor = new AddImport()
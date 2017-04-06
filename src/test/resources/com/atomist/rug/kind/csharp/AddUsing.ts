import {Project} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/RugOperation'
import {DecoratingPathExpressionEngine} from '@atomist/rug/ast/DecoratingPathExpressionEngine'
import {RichTextTreeNode} from '@atomist/rug/ast/TextTreeNodeOps'

class AddUsing implements ProjectEditor {
    name: string = "Constructed"
    description: string = "Adds using to C# file"

    edit(project: Project) {
      let eng: PathExpressionEngine = 
      new DecoratingPathExpressionEngine(project.context.pathExpressionEngine)

      let count = 0
      eng.with<RichTextTreeNode>(project, "//File()/CSharpFile()//using_directive[1]", n => {
        //console.log(`The using was '${n.value()}'`)
        n.append("\nusing System.Linq;")
        count++
      })

      if (count == 0)
       throw new Error("No C# files using directive found. Sad.")
    }
}

export let editor = new AddUsing()
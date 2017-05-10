import {Project,File} from '@atomist/rug/model/Core'
import {Editor} from '@atomist/rug/operations/Decorators'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/RugOperation'

@Editor("ChangeUsing")
class ChangeUsing  {

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context.pathExpressionEngine

      let count = 0
      eng.with<TextTreeNode>(project, "//File()/CSharpFile()//using_directive", n => {
        //console.log(`The import was '${n.value()}'`)
        n.update("using newImportWithAVeryVeryLongName;")
        count++
      })

      if (count == 0)
       throw new Error("No C# files with imports found. Sad.")
    }
}

export let editor = new ChangeUsing();

import {Project,File, CSharpFile} from '@atomist/rug/model/Core'
import {Editor} from '@atomist/rug/operations/Decorators'
import {PathExpression,TreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/RugOperation'

@Editor("Uses C# type")
class Imports  {

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context.pathExpressionEngine


      let count = 0
      eng.with<File>(project, "//File()[/CSharpFile()//using_directive]", f => {


    const csf: CSharpFile[] = (f as any).$jumpInto("CSharpFile");
     if (!csf) throw "Got null or undefined";
    if (csf.length == 0) throw "Got an empty array";
    if (csf[0].nodeTags().indexOf("compilation_unit") == -1)
        throw `This CSharpFile does not identify as a CSharpFile. Sad.`;

    const csf1: CSharpFile = (f as any).$jumpIntoOne("CSharpFile");
       if (!csf1) throw "Got null or undefined";
        if (csf1.nodeTags().indexOf("compilation_unit") == -1)
            throw `This CSharpFile does not identify as a CSharpFile. Sad.`;

        count++
      })

      if (count == 0)
       throw new Error("No C# files with imports found. Sad.")
    }

  }

export let editor = new Imports();

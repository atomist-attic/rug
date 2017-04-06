import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/Decorators'

class ChangeException implements ProjectEditor {
    name: string = "ChangeException"
    description: string = "Changes exception"

    @Parameter({pattern: "^.*$$", description: "New package"})
    newException: string

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context.pathExpressionEngine

      /*
        specific_catch_clause
	      : CATCH OPEN_PARENS class_type identifier? CLOSE_PARENS exception_filter? block
      */
      let catchClause = `/src//CSharpFile()//specific_catch_clause[//class_type[@value='IndexOutOfRangeException']]`

      let count = 0
      eng.with<TextTreeNode>(project, catchClause, cc => {
        //console.log(`The catch clause was '${cc.value()} at ${cc.formatInfo()}'`)
        if (cc.formatInfo == null)
          throw new Error(`Format info was null for ${cc.nodeName()}`)
        if (cc.formatInfo.start.lineNumberFrom1 < 5 || cc.formatInfo.start.lineNumberFrom1 > 100)
          throw new Error(`Format info values are wacky in ${cc.formatInfo}`)
        let c2 = cc as any // We need to do this to get to the children
        let classType = c2.class_type()
        if (classType.parent().value() != cc.value())
          throw new Error(`Unexpected value for parent of ${classType.nodeName()}: ${classType.parent()}`)
        classType.update(this.newException)
        count++
      })

      if (count == 0)
       throw new Error("No C# files with exceptions found. Sad.")
    }
}

export let editor = new ChangeException()
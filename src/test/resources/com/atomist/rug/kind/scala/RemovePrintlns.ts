import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {ScalaPathExpressionEngine} from '@atomist/rug/ast/scala/ScalaPathExpressionEngine'
import * as scala from '@atomist/rug/ast/scala/Types'

/**
 * Removes printlns
 */
class RemovePrintlns implements ProjectEditor {
    name: string = "RemovePrintlns"
    description: string = "Remove printlns"

    /*
      Our target looks like this:
     
      termApply:[ScalaMetaTreeBacked, -dynamic, termApply]
            termName:[println]
            termApplyInfix:[ScalaMetaTreeBacked, -dynamic, termApplyInfix]
              lit:[1]
              termName:[+]
              lit:[2]
     */
    edit(project: Project) {
      let eng: PathExpressionEngine =
        new ScalaPathExpressionEngine(project.context().pathExpressionEngine())

      let printlnStatement = 
        `/src/Directory()/scala//ScalaFile()//termApply
            [/termName[@value='println'] or contains(termSelect, 'System.out.println')]`   

      eng.with<scala.TermApply>(project, printlnStatement, termApply => {
        // console.log(`The term apply is ${termApply}`)
        termApply.delete()
      })
  }

}

export let editor = new RemovePrintlns()
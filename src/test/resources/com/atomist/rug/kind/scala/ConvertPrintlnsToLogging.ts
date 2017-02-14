import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Parameter, Editor} from '@atomist/rug/operations/Decorators'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {ScalaPathExpressionEngine} from '@atomist/rug/ast/scala/ScalaPathExpressionEngine'
import * as scala from '@atomist/rug/ast/scala/Types'

/**
 * Removes printlns to logging using a defined logger
 */
class ConvertPrintlnsToLogging implements ProjectEditor {
    name: string = "ConvertPrintlnsToLogging"
    description: string = "Convert printlns to logging"

    logStatement = "logger.debug"

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
        //console.log(`The term apply is ${termApply}`)
        let newContent = termApply.value()
            .replace("System.out.println", this.logStatement)
            .replace("println", this.logStatement)
        termApply.update(newContent)
      })
  }

}

export let editor = new ConvertPrintlnsToLogging()
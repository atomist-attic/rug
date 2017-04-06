import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'

/**
 * Name a parameter
 */
class NameParameter implements ProjectEditor {
    name: string = "NameParameter"
    description: string = "Name a parameter"

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context.pathExpressionEngine()

      /*
      termApply:[ScalaMetaTreeBacked, -dynamic]
            ctorRefName:[AntlrRawFileType]
            lit:["file_input"]
            termName:[FromGrammarAstNodeCreationStrategy]
            termArgNamed:[ScalaMetaTreeBacked, -dynamic]
              termName:[grammar]
              lit:["classpath:grammars/antlr/Python3.g4"]
      */
      let oldAssertion = `/src/Directory()/scala//ScalaFile()//termApply[/ctorRefName[@value='AntlrRawFileType']]/termName`

      eng.with<any>(project, oldAssertion, termName => {
          //console.log(`I am gonna update ${termName}`)
          termName.update(`nodeNamingStrategy = ${termName.value()}`)
      })
  }

}

export let editor = new NameParameter()
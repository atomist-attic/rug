import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {parameter} from '@atomist/rug/operations/RugOperation'

class UpgradeScalaTestAssertions implements ProjectEditor {
    name: string = "UpgradeScalaTestAssertions"
    description: string = "Upgrades ScalaTest assertions"

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context().pathExpressionEngine()

      /*
      We're matching a structure like this:

      TermApplyInfix:[MutableContainer]
              TermSelect:[MutableContainer]
                TermName:[scenarios]
                TermName:[size]
              TermName:[should]
              TermApply:[MutableContainer]
                TermName:[be]
                Lit:[2]
      */
      let oldAssertion = `/src/test/scala//ScalaFile()//TermApplyInfix[/TermName[@value='should']]`

      eng.with<TextTreeNode>(project, oldAssertion, shouldTerm => {
        //console.log(`The catch clause was '${cc.value()} at ${cc.formatInfo()}'`)
        
        // let c2 = cc as any // We need to do this to get to the children
        // let classType = c2.class_type()
        console.log(shouldTerm.value())
      })
    }
}

export let editor = new UpgradeScalaTestAssertions()
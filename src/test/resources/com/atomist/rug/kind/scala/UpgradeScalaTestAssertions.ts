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
      let oldAssertion = `/src/test/scala//ScalaFile()//termApplyInfix[/termName[@value='should']]`

      eng.with<any>(project, oldAssertion, shouldTerm => {

        console.log("b4 select")

        let termSelect = shouldTerm.termSelect()

        console.log("after select")
        let termApply = shouldTerm.termApply()
        if (termApply != null) {
          console.log(`after apply, termApply value = ${termApply.value()}`)
        }
        if (termApply != null && termApply.termName().value() == "be") {

          console.log(shouldTerm.value())
        }
        else {
          console.log(`after apply, termApply is null`)
        }

      })
}

}

export let editor = new UpgradeScalaTestAssertions()
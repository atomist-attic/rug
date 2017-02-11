import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'

/**
 * Update ScalaTest assertions of the form "a should be(b)" or a "should equal(b)"
 * with "assert(a === b)" to get better error messages.
 */
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
      let oldAssertion = `/src/test/scala//ScalaFile()//termApplyInfix[/termName[@value='should']][termSelect]`

      eng.with<any>(project, oldAssertion, shouldTerm => {

shouldTerm.dispatch_me = function(name) {
  console.log(name)
}
shouldTerm.dispatch_me("foo")
console.log("after bogus call")

        let termSelect = shouldTerm.termSelect()
        let termApply = shouldTerm.termApply()
        if (termApply != null && ["be", "equal"].indexOf(termApply.termName().value()) > -1) {
          let newValue = `assert(${termSelect.value()} === ${termApply.children()[1].value()})`
          //console.log(`Replacing [${shouldTerm.value()}] with [${newValue}]`)
          shouldTerm.update(newValue)
        }
      })
  }

}

export let editor = new UpgradeScalaTestAssertions()
import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'

/**
 * Upgrade Scala use of Java-style "a.equals(b)" to
 * more readable and idiomatic "a == b"
 */
class EqualsToSymbol implements ProjectEditor {
    name: string = "ConvertEqualsToSymbol"
    description: string = "Convert .equals to =="

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context().pathExpressionEngine()

      /*
      We're matching a structure like this:

      termApply:[ScalaMetaTreeBacked, -dynamic]
              termSelect:[ScalaMetaTreeBacked, -dynamic]
                termName:[a]
                termName:[equals]
              termName:[b]
      */
      let oldAssertion = `/src/test/scala//ScalaFile()//termApply[/termSelect/termName[2][@value='equals']][/termName]`

      eng.with<any>(project, oldAssertion, termApply => {

        let leftTerm = termApply.termSelect().children()[0]
        let rightTerm = termApply.termName()

        if (leftTerm && rightTerm) {
          //console.log(termApply)
          termApply.update(`${leftTerm.value()} == ${rightTerm.value()}`)
        }
      })
}

}

export let editor = new EqualsToSymbol()
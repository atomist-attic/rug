import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,PathExpressionEngine,TextTreeNode} from '@atomist/rug/tree/PathExpression'

/**
 * Upgrade Scala use of Java-style "a.equals(b)" to
 * more readable and idiomatic "a == b"
 */
class EqualsToSymbol implements ProjectEditor {
    name: string = "ConvertEqualsToSymbol"
    description: string = "Convert .equals to =="

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context.pathExpressionEngine

      /*
      We're matching a structure like this:

      a.equals(b)
      termApply:
              termSelect:
                termName:[a]
                termName:[equals]
              termName:[b]

              - or -

      ("dog" + "gie").equals("cat")
      termApply:
            termSelect:
              termApplyInfix:
                lit:["dog"]
                termName:[+]
                lit:["gie"]
              termName:[equals]
            lit:["cat"]
      */
      let oldAssertion = `/src/Directory()/scala//ScalaFile()//termApply[/termSelect/*[@value='equals']]`;

      eng.with<any>(project, oldAssertion, termApply => {
        //console.log(`Operating on ${termApply}`);
        if (termApply.children().length == 2) { // Should go in path expression when we have "count"
          let leftTerm = termApply.termSelect().children()[0]
          let rightTerm = termApply.children()[1]

          if (leftTerm && rightTerm) {
            let rightValue = rightTerm.children().length > 1 ? `(${rightTerm.value()})` : rightTerm.value();
            termApply.update(`${leftTerm.value()} == ${rightValue}`)
          }
        }
      })
  }

}

export const editor = new EqualsToSymbol()
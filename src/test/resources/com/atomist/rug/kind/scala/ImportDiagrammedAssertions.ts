import {Project,File} from '@atomist/rug/model/Core'
import {Editor} from '@atomist/rug/operations/Decorators'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'

/**
 * Import DiagrammedAssertions where ScalaTest is used
 */
@Editor("Imports ScalaTest DiagrammedAssertions where imports are used")
class ImportDiagrammedAssertions {

    newImport = "import org.scalatest.DiagrammedAssertions._"

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context.pathExpressionEngine
      let findExistingScalaTestImport = `/src/test/scala//ScalaFile()[not(//import[//termName[@value='DiagrammedAssertions']])]
                            //import[//termName[@value='scalatest']][1]`   

      eng.with<any>(project, findExistingScalaTestImport, existingImport => {
        let newValue = `${existingImport.value()}\n${this.newImport}`
        //console.log(`Replacing [${existingImport.value()}] with [${newValue}]`)
        existingImport.update(newValue)
        })
    }
}

export let editor = new ImportDiagrammedAssertions()

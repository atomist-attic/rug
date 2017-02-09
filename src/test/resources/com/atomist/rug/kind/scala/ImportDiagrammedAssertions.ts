import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'

/**
 * Import DiagrammedAssertions where ScalaTest is used
 */
class ImportDiagrammedAssertions implements ProjectEditor {
    name: string = "ImportsDiagrammedAssertions"
    description: string = "Imports ScalaTest DiagrammedAssertions where imports are used"

    newImport = "import org.scalatest.DiagrammedAssertions._"

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context().pathExpressionEngine()
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
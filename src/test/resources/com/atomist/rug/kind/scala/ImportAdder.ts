import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {ScalaHelper} from '@atomist/rug/scala/ScalaHelper'


/**
 * Uses our ScalaHelper to add imports
 */
class ImportAdder implements ProjectEditor {
    name: string = "UpgradeScalaTestAssertions"
    description: string = "Upgrades ScalaTest assertions"

    private scalaHelper = new ScalaHelper

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context().pathExpressionEngine()

      let findExistingScalaTestImport = `/src/Directory()/scala//ScalaFile()`   

      eng.with<any>(project, findExistingScalaTestImport, scalaFile => {
        this.scalaHelper.importIfNotImported(scalaFile, "org.scalatest.DiagrammedAssertions._")
      })
  }

}

export let editor = new ImportAdder()
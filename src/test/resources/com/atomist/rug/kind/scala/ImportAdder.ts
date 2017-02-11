import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {ScalaHelper} from '@atomist/rug/ast/scala/ScalaHelper'
import * as scala from '@atomist/rug/ast/scala/Types'

/**
 * Uses our ScalaHelper to add imports
 */
class ImportAdder implements ProjectEditor {
    name: string = "UpgradeScalaTestAssertions"
    description: string = "Upgrades ScalaTest assertions"

    edit(project: Project) {
      let eng: PathExpressionEngine = project.context().pathExpressionEngine()
      let scalaHelper = new ScalaHelper(eng)

      let findExistingScalaTestImport = `/src/Directory()/scala//ScalaFile()`   

      eng.with<scala.ScalaSource>(project, findExistingScalaTestImport, scalaFile => {
        let newScalaFile = scalaHelper.addImport(scalaFile, "org.scalatest.DiagrammedAssertions._")
        if (newScalaFile.value().indexOf("DiagrammedAssertions") < 0)
          throw new Error(`Content not right when i asked again: [${newScalaFile.value()}]`)
        // else
        //   console.log(`Content right when i asked again: [${newScalaFile.value()}]`)
      })
  }

}

export let editor = new ImportAdder()
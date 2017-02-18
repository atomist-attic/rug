import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import * as yaml from '@atomist/rug/ast/yaml/YamlPathExpressionEngine'

/**
 * Uses Scala mixin add imports
 */
class ChangeQuoted implements ProjectEditor {
    name: string = "ChangeQuoted"
    description: string = "Adds import"

    edit(project: Project) {
      let eng: PathExpressionEngine =
        new yaml.YamlPathExpressionEngine(project.context().pathExpressionEngine())

      let findDependencies = `/*[@name='x.yml']/YmlFile()/dependencies/*[contains(., "Bohemian")]`   

      eng.with<yaml.YamlValue>(project, findDependencies, ymlValue => {
        //console.log(`Raw value is [${ymlValue.value()}]`)
        if (!(ymlValue.value().charAt(0) == '"'))
            throw new Error(`[${ymlValue.value()}] doesn't start with "`)
        ymlValue.updateText("White Rabbit")
      })
  }

}

export let editor = new ChangeQuoted()
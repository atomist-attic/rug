import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

import * as yaml from '@atomist/rug/ast/yaml/Types'
import {YamlPathExpressionEngine} from '@atomist/rug/ast/yaml/YamlPathExpressionEngine'

class ChangeQuoted implements ProjectEditor {
    name: string = "ChangeQuoted"
    description: string = "Adds import"

    edit(project: Project) {
      let eng: PathExpressionEngine =
        new YamlPathExpressionEngine(project.context().pathExpressionEngine())

      let findDependencies = `/*[@name='x.yml']/YamlFile()/dependencies/*[contains(., "Bohemian")]`

      eng.with<yaml.YamlString>(project, findDependencies, ymlValue => {
        //console.log(`Raw value is [${ymlValue.value()}]`)
        if (ymlValue.value() != `"Bohemian Rhapsody"`)
            throw new Error(`[${ymlValue.value()}] doesn't contain "`)
        if (ymlValue.text() != "Bohemian Rhapsody")
            throw new Error(`[${ymlValue.text()}] DOES contain "`)
        ymlValue.updateText("White Rabbit")
      })
  }

}

export let editor = new ChangeQuoted()
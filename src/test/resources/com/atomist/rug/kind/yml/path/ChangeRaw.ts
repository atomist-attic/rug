import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import * as yaml from '@atomist/rug/ast/yaml/Types'
import {YamlPathExpressionEngine} from '@atomist/rug/ast/yaml/YamlPathExpressionEngine'

class ChangeRaw implements ProjectEditor {
    name: string = "ChangeRaw"
    description: string = "Adds import"

    edit(project: Project) {
      let eng: PathExpressionEngine =
        new YamlPathExpressionEngine(project.context().pathExpressionEngine())

      let findDependencies = `/*[@name='x.yml']/YmlFile()/group/value`   

      eng.with<yaml.YamlString>(project, findDependencies, ymlValue => {
        //console.log(`Raw value is [${ymlValue.value()}]`)
        if (ymlValue.value() != "queen")
            throw new Error(`[${ymlValue.value()}] not 'queen'"`)
        if (ymlValue.text() != "queen")
            throw new Error(`[${ymlValue.text()}] not 'queen'`)
        ymlValue.updateText("Jefferson Airplane")
      })
  }

}

export let editor = new ChangeRaw()
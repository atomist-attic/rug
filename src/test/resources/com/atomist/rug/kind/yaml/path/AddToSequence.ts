import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

import * as yaml from '@atomist/rug/ast/yaml/Types'
import {YamlPathExpressionEngine} from '@atomist/rug/ast/yaml/YamlPathExpressionEngine'

class AddToSequence implements ProjectEditor {
    name: string = "AddToSequence"
    description = "Add to sequence"

    edit(project: Project) {
      let eng: PathExpressionEngine =
        new YamlPathExpressionEngine(project.context().pathExpressionEngine())

      let findDependencies = `/*[@name='x.yml']/YamlFile()/dependencies`

      eng.with<TextTreeNode>(project, findDependencies, yamlValue => {
        //console.log(`Raw value is [${yamlValue.value()}]`)
        console.log(`Sequence value is [${yamlValue.value()}]`)
        // if (yamlValue.value().charAt(0) != ">")
        //     throw new Error(`[${yamlValue.value()}] doesn't start with >`)
        // if (yamlValue.text().charAt(0) == ">")
        //     throw new Error(`[${yamlValue.text()}] DOES start with >`)
        // yamlValue.updateText(this.newComment)
      })
  }

}

export let editor = new AddToSequence()
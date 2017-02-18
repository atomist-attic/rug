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

      let findDependencies = `/*[@name='x.yml']/YmlFile()/dependencies`   

      eng.with<TextTreeNode>(project, findDependencies, ymlValue => {
        //console.log(`Raw value is [${ymlValue.value()}]`)
        console.log(`Sequence value is [${ymlValue.value()}]`)
        // if (ymlValue.value().charAt(0) != ">")
        //     throw new Error(`[${ymlValue.value()}] doesn't start with >`)
        // if (ymlValue.text().charAt(0) == ">")
        //     throw new Error(`[${ymlValue.text()}] DOES start with >`)
        // ymlValue.updateText(this.newComment)
      })
  }

}

export let editor = new AddToSequence()
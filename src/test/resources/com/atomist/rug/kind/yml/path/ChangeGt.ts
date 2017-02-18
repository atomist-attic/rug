import {Project,File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {EventHandler, ResponseHandler, CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
import {PathExpression,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

import * as yaml from '@atomist/rug/ast/yaml/Types'

import {YamlPathExpressionEngine} from '@atomist/rug/ast/yaml/YamlPathExpressionEngine'

class ChangeGt implements ProjectEditor {
    name: string = "ChangeGt"
    description = "Change > string"

    @Parameter({description: "Change comment to this", pattern: "^.*$"})
    newComment: string

    edit(project: Project) {
      let eng: PathExpressionEngine =
        new YamlPathExpressionEngine(project.context().pathExpressionEngine())

      let findDependencies = `/*[@name='x.yml']/YmlFile()/comments`   

      eng.with<yaml.YamlString>(project, findDependencies, ymlValue => {
        //console.log(`Raw value is [${ymlValue.value()}]`)
        console.log(`Text value is [${ymlValue.text()}]`)
        if (ymlValue.value().charAt(0) != ">")
            throw new Error(`[${ymlValue.value()}] doesn't start with >`)
        if (ymlValue.text().charAt(0) == ">")
            throw new Error(`[${ymlValue.text()}] DOES start with >`)
        ymlValue.updateText(this.newComment)
      })
  }
}

export let editor = new ChangeGt()
import {Project, File} from "@atomist/rug/model/Core";
import {Editor} from "@atomist/rug/operations/Decorators";
import {PathExpression, TextTreeNode, TypeProvider, PathExpressionEngine} from "@atomist/rug/tree/PathExpression";
import * as yaml from "@atomist/rug/ast/yaml/Types";
import {YamlPathExpressionEngine} from "@atomist/rug/ast/yaml/YamlPathExpressionEngine";

@Editor("Adds import")
class ChangeRaw  {

    edit(project: Project) {
        let eng: PathExpressionEngine =
            new YamlPathExpressionEngine(project.context.pathExpressionEngine)

        let findGroup = `/*[@name='x.yml']/YamlFile()/group/value`

        eng.with<yaml.YamlString>(project, findGroup, yamlValue => {
            //console.log(`Raw value is [${yamlValue.value()}]`)
            if (yamlValue.value() != "queen")
                throw new Error(`[${yamlValue.value()}] not 'queen'"`)
            if (yamlValue.text() != "queen")
                throw new Error(`[${yamlValue.text()}] not 'queen'`)

            yamlValue.updateText("Jefferson Airplane")
            // console.log(`${this.description}: updated text value is [${yamlValue.text()}]`)
        })
    }

}

export let editor = new ChangeRaw()

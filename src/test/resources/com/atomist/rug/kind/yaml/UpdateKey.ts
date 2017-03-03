import {Project, File} from "@atomist/rug/model/Core";
import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor";
import {PathExpression, TextTreeNode, TypeProvider, PathExpressionEngine} from "@atomist/rug/tree/PathExpression";
import * as yaml from "@atomist/rug/ast/yaml/Types";
import {YamlPathExpressionEngine} from "@atomist/rug/ast/yaml/YamlPathExpressionEngine";

class UpdateKey implements ProjectEditor {
    name: string = "UpdateKey"
    description: string = "Changes a key"

    edit(project: Project) {
        let eng: PathExpressionEngine =
            new YamlPathExpressionEngine(project.context().pathExpressionEngine())

        let findDependencies = `/*[@name='x.yml']/YamlFile()/*[@name='dependencies']`

        eng.with<yaml.YamlUpdateKey>(project, findDependencies, yamlValue => {
            // console.log(`Raw value is [${yamlValue.value()}]`)
            yamlValue.updateKey("songs")
            // console.log(`${this.description}: updated text value is [${yamlValue.text()}]`)
        })
    }

}

export let editor = new UpdateKey()
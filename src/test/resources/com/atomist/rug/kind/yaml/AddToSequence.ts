import {Project, File} from "@atomist/rug/model/Core";
import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor";
import {PathExpression, TextTreeNode, TypeProvider, PathExpressionEngine} from "@atomist/rug/tree/PathExpression";
import * as yaml from "@atomist/rug/ast/yaml/Types";
import {YamlPathExpressionEngine} from "@atomist/rug/ast/yaml/YamlPathExpressionEngine";

class AddToSequence implements ProjectEditor {
    name: string = "AddToSequence"
    description = "Add to sequence"

    edit(project: Project) {
        let eng: PathExpressionEngine =
            new YamlPathExpressionEngine(project.context().pathExpressionEngine())

        let findDependencies = `/*[@name='x.yml']/YamlFile()/dependencies`

        eng.with<yaml.YamlSequence>(project, findDependencies, yamlValue => {
            yamlValue.addElement('"Killer Queen"')
            // console.log(`${this.description}: updated text value is \n[${yamlValue.value()}]`)
        })
    }

}

export let editor = new AddToSequence()
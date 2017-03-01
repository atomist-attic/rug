import {Project, File} from "@atomist/rug/model/Core";
import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor";
import {PathExpression, TextTreeNode, TypeProvider, PathExpressionEngine} from "@atomist/rug/tree/PathExpression";
import * as yaml from "@atomist/rug/ast/yaml/Types";
import {YamlPathExpressionEngine} from "@atomist/rug/ast/yaml/YamlPathExpressionEngine";

class RemoveFromDeepNestedSequence implements ProjectEditor {
    name: string = "RemoveFromSequence"
    description = "Remove from sequence"

    edit(project: Project) {
        let eng: PathExpressionEngine =
            new YamlPathExpressionEngine(project.context().pathExpressionEngine())

        let findDependencies = `/*[@name='x.yml']/YamlFile()/components/Amplifier/*[@name='future upgrades']/NAC82`

        eng.with<yaml.Sequence>(project, findDependencies, yamlValue => {
            yamlValue.removeElement('Hicap')
            // console.log(`${this.description}: updated text value is \n[${yamlValue.value()}]`)
        })
    }

}

export let editor = new RemoveFromDeepNestedSequence()
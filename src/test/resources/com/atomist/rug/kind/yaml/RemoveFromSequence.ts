import {Project, File} from "@atomist/rug/model/Core";
import {Editor} from "@atomist/rug/operations/Decorators";
import {PathExpression, TextTreeNode, TypeProvider, PathExpressionEngine} from "@atomist/rug/tree/PathExpression";
import * as yaml from "@atomist/rug/ast/yaml/Types";
import {YamlPathExpressionEngine} from "@atomist/rug/ast/yaml/YamlPathExpressionEngine";

@Editor("Edits")
class RemoveFromSequence  {

    edit(project: Project) {
        let eng: PathExpressionEngine =
            new YamlPathExpressionEngine(project.context.pathExpressionEngine)

        let findDependencies = `/*[@name='x.yml']/YamlFile()/dependencies`

        eng.with<yaml.YamlSequence>(project, findDependencies, yamlValue => {
            yamlValue.removeElement('"Sweet Lady"')
            // console.log(`${this.description}: updated text value is \n[${yamlValue.value()}]`)
        })
    }

}

export let editor = new RemoveFromSequence()

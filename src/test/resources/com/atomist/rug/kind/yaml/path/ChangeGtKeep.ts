import {Project, File} from "@atomist/rug/model/Core";
import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor";
import {EventHandler, ResponseHandler, CommandHandler, Parameter, Tags, Intent} from "@atomist/rug/operations/Decorators";
import {PathExpression, TextTreeNode, TypeProvider, PathExpressionEngine} from "@atomist/rug/tree/PathExpression";
import * as yaml from "@atomist/rug/ast/yaml/Types";
import {YamlPathExpressionEngine} from "@atomist/rug/ast/yaml/YamlPathExpressionEngine";

class ChangeGtKeep implements ProjectEditor {
    name: string = "ChangeGtKeep"
    description = "Change >+ string"

    @Parameter({description: "Change comment to this", pattern: "^[\\s\\S]*$"})
    newComment: string

    edit(project: Project) {
        let eng: PathExpressionEngine =
            new YamlPathExpressionEngine(project.context().pathExpressionEngine())

        let findDependencies = `/*[@name='x.yml']/YamlFile()/comments`

        eng.with<yaml.YamlString>(project, findDependencies, yamlValue => {
            console.log(`Text value is [${yamlValue.text()}]`)
            if (yamlValue.value().charAt(0) != ">" && yamlValue.value().charAt(1) != "+")
                throw new Error(`[${yamlValue.value()}] doesn't start with >-`)
            if (yamlValue.text().charAt(0) == ">" && yamlValue.text().charAt(1) == "+")
                throw new Error(`[${yamlValue.text()}] DOES start with >+`)

            yamlValue.updateText(this.newComment)
            // console.log(`${this.description}: updated text value is [${yamlValue.text()}]`)
        })
    }
}

export let editor = new ChangeGtKeep()
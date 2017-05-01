import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
import {Project, Pair} from '@atomist/rug/model/Core'
import {Match, PathExpression, PathExpressionEngine, TreeNode} from '@atomist/rug/tree/PathExpression'

class Rename3 implements ProjectEditor {
    name: string = "rename"
    description: string = "Rename"

    edit(project: Project) {
        let eng: PathExpressionEngine = project.context.pathExpressionEngine;
        project.files.forEach(f => {
            if (f.filename == "package.json")
                eng.with<Pair>(f, "/Json()/subdomain", d => {
                    d.setValue("absquatulate")
                })
        })
    }
}

export const rename3 = new Rename3();
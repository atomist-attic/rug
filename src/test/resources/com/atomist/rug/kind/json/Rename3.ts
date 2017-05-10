import {Editor} from "@atomist/rug/operations/Decorators"
import {Project, Pair} from '@atomist/rug/model/Core'
import {Match, PathExpression, PathExpressionEngine, TreeNode} from '@atomist/rug/tree/PathExpression'

@Editor("rename", "rename")
class Rename3 {
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

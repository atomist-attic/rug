import { Project, Line } from '@atomist/rug/model/Core'
import { ProjectReviewer } from '@atomist/rug/operations/ProjectReviewer'
import { PathExpression, PathExpressionEngine } from '@atomist/rug/tree/PathExpression'
import { ReviewResult, ReviewComment, Severity } from '@atomist/rug/operations/RugOperation'
import { DecoratingPathExpressionEngine } from '@atomist/rug/ast/DecoratingPathExpressionEngine'
import { RichTextTreeNode } from '@atomist/rug/ast/TextTreeNodeOps'
import { Parameter, Reviewer } from '@atomist/rug/operations/Decorators'

export class FindCorruption implements ProjectReviewer {

    name = "FindCorruption"

    description = "Look for corrupt politicians"

    review(project: Project) {
        let eng: PathExpressionEngine = project.context().pathExpressionEngine()

        let rr = ReviewResult.empty(this.name)

        eng.with<Line>(project, "//Line()", l => {
            //console.log(l)
            let index = l.value().indexOf("Burke")
            if (index > -1)
                rr.add(new ReviewComment(
                    this.name,
                    Severity.Major,
                    l.file().path,
                    l.numFrom1(),
                    index + 1
                )
            )
        })

        return rr
    }
}

export let editor = new FindCorruption()
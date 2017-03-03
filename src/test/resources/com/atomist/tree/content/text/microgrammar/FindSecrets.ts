import { Project, Line } from '@atomist/rug/model/Core'
import { ProjectReviewer } from '@atomist/rug/operations/ProjectReviewer'
import { PathExpression, PathExpressionEngine } from '@atomist/rug/tree/PathExpression'
import { ReviewResult, ReviewComment, Severity } from '@atomist/rug/operations/RugOperation'
import { DecoratingPathExpressionEngine } from '@atomist/rug/ast/DecoratingPathExpressionEngine'
import { RichTextTreeNode } from '@atomist/rug/ast/TextTreeNodeOps'
import { Parameter, Reviewer } from '@atomist/rug/operations/Decorators'

export class FindSecrets implements ProjectReviewer {

    name = "FindSecrets"

    description = "Look for secrets"

    review(project: Project) {

        let rr = ReviewResult.empty(this.name)

        project.files()
            .filter(f => f.name()
            .indexOf("yml") > -1).forEach(f => {
            var secret = /\$\{secret\.([^\}]+)\}/g;
            var matches = f.content().match(secret);
            for ( let i = 0; i < matches.length; i++)
                rr.add(new ReviewComment(
                    matches[i],
                    Severity.Major,
                    f.path()
                ))
        })

        return rr
    }
}

export let editor = new FindSecrets()
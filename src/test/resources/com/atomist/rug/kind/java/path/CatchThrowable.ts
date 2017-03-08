import {Project} from '@atomist/rug/model/Core'
import {ProjectReviewer} from '@atomist/rug/operations/ProjectReviewer'
import {PathExpression,PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {ReviewResult,ReviewComment,Severity} from '@atomist/rug/operations/RugOperation'
import {DecoratingPathExpressionEngine} from '@atomist/rug/ast/DecoratingPathExpressionEngine'
import {RichTextTreeNode} from '@atomist/rug/ast/TextTreeNodeOps'
import {Parameter, Reviewer} from '@atomist/rug/operations/Decorators'

import * as java from '@atomist/rug/ast/java/Types'

export class CatchThrowable implements ProjectReviewer {

    name = "CatchThrowable"

    description = "Look for particular throwables"

    @Parameter({description: "Exception to look for", pattern: "@java_identifier"})
    exception: string

    review(project: Project) {
      const eng = 
      new DecoratingPathExpressionEngine(project.context().pathExpressionEngine())

      const rr = ReviewResult.empty(this.name)

      eng.withExpression<RichTextTreeNode>(project, new java.Catch(this.exception), n => {      
        rr.add(n.commentConcerning(
                    this.name,
                    Severity.Major)
        )
       })

       eng.withExpression<RichTextTreeNode>(project, java.CatchException, n => {
         throw new Error("Shouldn't have caught Exception")
       })

       return rr
    }
}

export const editor = new CatchThrowable()
import {Project} from '@atomist/rug/model/Core'
import {ProjectReviewer} from '@atomist/rug/operations/ProjectReviewer'
import {PathExpression,PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {ReviewResult,ReviewComment,Severity} from '@atomist/rug/operations/RugOperation'
import {DecoratingPathExpressionEngine} from '@atomist/rug/ast/DecoratingPathExpressionEngine'
import {RichTextTreeNode} from '@atomist/rug/ast/TextTreeNodeOps'
import {Parameter, Reviewer} from '@atomist/rug/operations/Decorators'

import * as java from '@atomist/rug/ast/java/Types'

class CatchThrowable implements ProjectReviewer {

    name = "CatchThrowable"

    description = "Look for particular throwables"

    @Parameter({description: "Exception to look for", pattern: "^.*$"})
    exception: string

    review(project: Project) {
      let eng = 
      new DecoratingPathExpressionEngine(project.context().pathExpressionEngine())

      let rr = ReviewResult.empty(this.name)

      eng.withExpression<RichTextTreeNode>(project, new java.Catch(this.exception), n => {
        //console.log(`The using was '${n.value()}'`)
      
        rr.add(n.commentConcerning(
                    this.name,
                    Severity.Major)
        )
       })

       eng.withExpression<RichTextTreeNode>(project, java.CatchException, n => {
         throw new Error("Shouldn't have caught exception")
       })

       return rr
    }
}

export let editor = new CatchThrowable()
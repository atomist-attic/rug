import {Project} from '@atomist/rug/model/Core'
import {ProjectReviewer} from '@atomist/rug/operations/ProjectReviewer'
import {PathExpression,PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Parameter,ReviewResult,ReviewComment,Severity} from '@atomist/rug/operations/RugOperation'
import {DecoratingPathExpressionEngine} from '@atomist/rug/ast/DecoratingPathExpressionEngine'
import {RichTextTreeNode} from '@atomist/rug/ast/TextTreeNodeOps'

class CatchThrowable implements ProjectReviewer {
    name: string = "AddUsing"
    description: string = "Find "

    review(project: Project) {
      let eng: PathExpressionEngine = 
      new DecoratingPathExpressionEngine(project.context().pathExpressionEngine())

      let rr = ReviewResult.empty(this.name)

      eng.with<RichTextTreeNode>(project, "//JavaFile()//catchClause//catchType[@value='ThePlaneHasFlownIntoTheMountain']", n => {
        //console.log(`The using was '${n.value()}'`)
      
        rr.add(n.commentConcerning(
                    this.name,
                    Severity.Major)
        )
       })

       return rr
    }
}

export let editor = new CatchThrowable()
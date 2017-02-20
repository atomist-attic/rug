import {Project} from '@atomist/rug/model/Core'
import {ProjectReviewer} from '@atomist/rug/operations/ProjectReviewer'
import {Line} from '@atomist/rug/model/Core'
import {PathExpressionEngine,TextTreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {ReviewResult, ReviewComment, Severity} from '@atomist/rug/operations/RugOperation'
import {Parameter, Reviewer} from '@atomist/rug/operations/Decorators'
import {DecoratingPathExpressionEngine} from '@atomist/rug/ast/DecoratingPathExpressionEngine'


@Reviewer("Simple", "A nice little editor")
class FindLongLines implements ProjectReviewer {
    name: string = "Simple"
    description: string = "A nice little reviewer"

    @Parameter({description: "Max line length", pattern: "^.*$"})
    maxLength: number = 100

    review(project: Project) {
     
        let eng: PathExpressionEngine = 
            new DecoratingPathExpressionEngine(project.context().pathExpressionEngine())

        let comments: ReviewComment[] = []

        let longLines = `//File()/Line()`   

      eng.with<Line>(project, longLines, l => {
          //console.log(`Checking [${l}]`)
          if (l.length() > this.maxLength) {
            let rc = new ReviewComment(
                    this.name,
                    Severity.Major,
                    l.file().path(),
                    l.num(),
                    1)
            comments.push(rc)
        }
       
      })

       return new ReviewResult(this.name, comments)
    }
  }

export let reviewer = new FindLongLines()
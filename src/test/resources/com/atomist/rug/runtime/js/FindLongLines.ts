import {Project} from '@atomist/rug/model/Core'
import {ProjectReviewer} from '@atomist/rug/operations/ProjectReviewer'
import {File} from '@atomist/rug/model/Core'
import {ReviewResult, ReviewComment, Severity} from '@atomist/rug/operations/RugOperation'
import {Parameter, Reviewer} from '@atomist/rug/operations/Decorators'

@Reviewer("Simple", "A nice little editor")
class FindLongLines implements ProjectReviewer {
    name: string = "Simple"
    description: string = "A nice little reviewer"

    @Parameter({description: "Max line length", pattern: "^.*$"})
    maxLength: number = 100

    review(project: Project, {content} : {content: string}) {
      return new ReviewResult(content,
           [new ReviewComment(
            content,
            Severity.Broken,
            "file.txt",
            1,
            1)]
        );
    }
  }

export let reviewer = new FindLongLines()
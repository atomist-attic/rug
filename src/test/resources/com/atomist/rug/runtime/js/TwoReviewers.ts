import {Project} from '@atomist/rug/model/Core'
import {ProjectReviewer} from '@atomist/rug/operations/ProjectReviewer'
import {File} from '@atomist/rug/model/Core'
import {ReviewResult, ReviewComment, Parameter, Severity} from '@atomist/rug/operations/RugOperation'

class ReviewerA implements ProjectReviewer {

    name: string = "ReviewerA"
    description: string = "A nice little reviewer"

    review(project: Project) {
      return new ReviewResult("ReviewerA",
          [new ReviewComment("A", Severity.Broken)]
        );
    }
  }
export let reviewerA = new ReviewerA()


class ReviewerB implements ProjectReviewer {

    name: string = "ReviewerB"
    description: string = "A nice little reviewer"

    review(project: Project) {
      return new ReviewResult("ReviewerB",
          [new ReviewComment("B", Severity.Broken)]
        );
    }
  }
export let reviewerB = new ReviewerB()

class ReviewerC implements ProjectReviewer {

    name: string = "ReviewerC"
    description: string = "A nice little reviewer"

    review(project: Project) {
      return ReviewResult.combine ([
          reviewerA.review(project), 
          reviewerB.review(project)
      ])
    }
  }

export let reviewerC = new ReviewerC()

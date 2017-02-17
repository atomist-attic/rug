package com.atomist.project.review

import com.atomist.param.ParameterValues
import com.atomist.project.review.Severity.Severity
import com.atomist.project.ProjectDelta
import com.atomist.source.ArtifactSource

case class ReviewResult(
                       note: String,
                       comments: Seq[ReviewComment] = Nil
                       ) {

  /**
    * @return greatest severity we've encountered
    */
  def severity: Severity =
    comments.map(c => c.severity)
      .reduceLeftOption((s1, s2) => if (s1.id > s2.id) s1 else s2)
      .getOrElse(Severity.FINE)
}

case class ReviewComment(
                         comment: String,
                         severity: Severity,
                         fileName: Option[String] = None,
                         line: Option[Int] = None,
                         column: Option[Int] = None
                       )

/**
  * Reviews a project
  */
trait ProjectReviewer extends ProjectDelta {

  def review(as: ArtifactSource,
            pos: ParameterValues): ReviewResult
}

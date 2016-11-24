package com.atomist.project.review

import com.atomist.project.review.Severity._
import org.scalatest.{FlatSpec, Matchers}

class ReviewCommentTest extends FlatSpec with Matchers {

  it should "compute severity of issues" in {
    val rc1 = ReviewComment("comment", MAJOR, Some("file"))
    val rc2 = ReviewComment("comment2", POLISH, Some("file"))
    val rc3 = ReviewComment("comment2", BROKEN, Some("file"))

    val rr = ReviewResult("", Seq(rc1))
    rr.severity should equal(MAJOR)
    val rr2 = ReviewResult("", Seq(rc1, rc2))
    rr2.severity should equal(MAJOR)
    val rr3 = ReviewResult("", Seq(rc1, rc2, rc3))
    rr3.severity should equal(BROKEN)
  }

  it should "compute severity without issues" in {
    val rr = ReviewResult("nothing")
    rr.severity should be(FINE)
  }
}

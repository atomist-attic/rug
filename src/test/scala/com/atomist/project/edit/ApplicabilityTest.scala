package com.atomist.project.edit

import org.scalatest.{FlatSpec, Matchers}

class ApplicabilityTest extends FlatSpec with Matchers {

  "Applicability" should "support &&" in {
    val t1 = Applicability(canApply = true, "f1")
    val t2 = Applicability(canApply = true, "f2")
    val f1 = Applicability(canApply = false, "whatever")
    val f2 = Applicability(canApply = false, "whatever else")

    (t1 && t2).canApply should equal(true)
    (t1 && f1).canApply should equal(false)
    (t1 && t1 && t2).canApply should equal(true)
    (t1 && t1 && f1).canApply should equal(false)
  }
}

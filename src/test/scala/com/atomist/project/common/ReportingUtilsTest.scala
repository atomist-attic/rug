package com.atomist.project.common

import org.scalatest.{FlatSpec, Matchers}

class ReportingUtilsTest extends FlatSpec with Matchers {

  "ReportingUtils.withLineNumbers" should "handle single line" in {
    val in = "The quick brown fox jumped over the lazy dog"
    val out = ReportingUtils.withLineNumbers(in)
    out should equal("1 " + in)
  }

  it should "handle null" in {
    ReportingUtils.withLineNumbers(null) shouldBe null
  }

  it should "handle multiple lines" in {
    val in =
      """a
        |b
        |c""".stripMargin

    val expected =
      """1 a
        |2 b
        |3 c""".stripMargin
    val out = ReportingUtils.withLineNumbers(in)
    out should equal(expected)
  }
}

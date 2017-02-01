package com.atomist.tree.content.text.microgrammar

import org.scalatest.{FlatSpec, Matchers}

class DismatchReportTest extends FlatSpec with Matchers {

  it should "print something cool when it doesn't match" in {
    val aWasaByeah = new MatcherMicrogrammar(
      Regex("[A-Z][a-z]+", Some("name")) ~? Literal("was aged") ~? Regex("[0-9]+", Some("age")) ~ Literal("!")
    )
    val input = "Tony was aged 24. Alice was aged 16. And they are both gone"

    val Left(dismatchReport) = aWasaByeah.strictMatch(input)
    val output = DismatchReport.detailedReport(dismatchReport, input)

    val expected = """ [[name=Tony] was aged [age=24]]{.} Alice was aged 16. And they are both gone"""
    output.take(expected.length) should equal(expected)
    // Christian suggests this instead of {.}, and it is better:
    //    """ [[Tony] was aged [24]]. Alice was aged 16. And they are both gone
    //      |                       ~""".stripMargin) should be(true)
  }
}


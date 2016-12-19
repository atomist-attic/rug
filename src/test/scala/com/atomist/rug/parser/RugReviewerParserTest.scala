package com.atomist.rug.parser

import com.atomist.rug.RugReviewer
import org.scalatest.{FlatSpec, Matchers}

class RugReviewerParserTest extends FlatSpec with Matchers {

  val ri = new ParserCombinatorRugParser

  it should "parse simplest program" in
    simplestProgram("'Description'")

  private def simplestProgram(description: String = ""): RugReviewer = {
    val prog =
      s"""
         |@description $description
         |reviewer FindEJB
         |
           |with File f
         | when isJava and { f.contains("javax.ejb")};
         |
           |do
         | warn { "ejb found in " + f.name() };
    """.stripMargin
    val pops = ri.parse(prog)
    pops.size should be (1)
    pops.head match {
      case rr: RugReviewer => rr
    }
  }

}

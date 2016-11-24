package com.atomist.rug.parser

import com.atomist.rug.RugProjectPredicate
import org.scalatest.{FlatSpec, Matchers}

class RugPredicateParserTest extends FlatSpec with Matchers {

  val ri = new ParserCombinatorRugParser

  it should "parse simple with" in {
    val prog =
      s"""
         |predicate UsesEJB
         |
         |with file when isJava
    """.stripMargin
    val pops = ri.parse(prog)
    pops.size should be(1)
    pops.head match {
      case rpr: RugProjectPredicate => rpr
    }
  }

  it should "parse nested with" in {
    val prog =
      s"""
         |predicate UsesEJB
         |
         |with project when name contains "foo"
         |   with file when name = "Foo"
    """.stripMargin
    val pops = ri.parse(prog)
    pops.size should be(1)
    pops.head match {
      case rpr: RugProjectPredicate => rpr
    }
  }

}

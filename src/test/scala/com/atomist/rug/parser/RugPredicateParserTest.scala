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
         |with File when isJava
    """.stripMargin
    val pops = ri.parse(prog)
    assert(pops.size === 1)
    pops.head match {
      case rpr: RugProjectPredicate => rpr
    }
  }

  it should "parse nested with" in {
    val prog =
      s"""
         |predicate UsesEJB
         |
         |with Project when name contains "foo"
         |   with File when name = "Foo"
    """.stripMargin
    val pops = ri.parse(prog)
    assert(pops.size === 1)
    pops.head match {
      case rpr: RugProjectPredicate => rpr
    }
  }

}

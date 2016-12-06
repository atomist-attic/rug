package com.atomist.tree.pathexpression

import org.scalatest.{FlatSpec, Matchers}

class PathExpressionParserTest extends FlatSpec with Matchers {

  val pep = PathExpressionParser

  it should "parse expression with type jump" in {
    val pe = "/issue/test1:repo/project/src/main/java//*:file->java.class"
    val parsed = pep.parsePathExpression(pe)
    //println(parsed)
  }

}

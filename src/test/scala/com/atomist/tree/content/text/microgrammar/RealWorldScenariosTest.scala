package com.atomist.tree.content.text.microgrammar

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by rod on 10/29/16.
  */
class RealWorldScenariosTest extends FlatSpec with Matchers {

  private def javaIdentifier(name: String) = Regex(name, "[a-zA-Z][a-zA-Z0-9]*")

  it should "match JavaIdentifiers" in {
    javaIdentifier("foo").matchPrefix(0, "uuuuer23") match {
      case Some(m) =>
    }
  }

  it should "match Scala method header without args in whole input" in {
    val scalaMethodHeader =
      Literal("def") ~~ javaIdentifier("name") ~? Literal("(") ~? Literal(")") ~? Literal(":") ~? javaIdentifier("returnType")

    val formats = Seq(
      "def myMethod(): Int",
      "def myMethod ( )       :Int",
      """def myMethod ( )
         : Int"
      """.stripMargin
    )
    formats.map(noArgs =>
      scalaMethodHeader.matchPrefix(0, noArgs) match {
        case Some(PatternMatch(tn, 0, matched, `noArgs`, _)) =>
        //println(s"Successfully parsed [$matched]")
      })
  }

  it should "match Scala method header with and without args in whole input" in {
    val arg = javaIdentifier("name") ~? Literal(":") ~? javaIdentifier("type")
    val args  = Repsep(arg, Literal(","), "args")
    val scalaMethodHeader =
      Literal("def") ~~ javaIdentifier("returnType") ~? Literal("(") ~? args ~? Literal(")") ~? Literal(":") ~? javaIdentifier("returnType")

    val formats = Seq(
      "def myMethod(): Int",
      "def myMethod(a: Int, b: Int): Int",
      "def myMethod (c: String )       :Int",
      """def myMethod ( )
         : Int"
      """.stripMargin
    )
    formats.map(input =>
      scalaMethodHeader.matchPrefix(0, input) match {
        case Some(PatternMatch(tn, 0, matched, `input`, _)) =>
        case None => fail(s"Failed to match input [$input]")
      })
  }

  it should "match element content" in {
    val pattern = Literal("<title>") ~ Break(Literal("</title>"))

    val content = "this is a test"
    val formats = Seq(
      s"<title>$content</title>"
    )
    formats.map(input =>
      pattern.matchPrefix(0, input) match {
        case Some(PatternMatch(tn, 0, matched, `input`, _)) =>
        //println(s"Successfully parsed [$matched]")
      })

  }

}

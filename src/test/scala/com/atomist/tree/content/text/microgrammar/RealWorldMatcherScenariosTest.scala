package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.microgrammar.matchers.Break
import org.scalatest.{FlatSpec, Matchers}

class RealWorldMatcherScenariosTest extends FlatSpec with Matchers {

  private def javaIdentifier(name: String): Matcher = Regex("[a-zA-Z][a-zA-Z0-9]*", Some(name))

  it should "match JavaIdentifiers" in {
    javaIdentifier("foo").matchPrefix(InputState("uuuuer23")) match {
      case Right(m) =>
      case _ => ???
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
      scalaMethodHeader.matchPrefix(InputState(noArgs)) match {
        case Right(PatternMatch(tn, matched, InputState2(`noArgs`, _, _), _)) =>
        //println(s"Successfully parsed [$matched]")
        case _ => ???
      })
  }

  it should "match Scala method header with and without args in whole input" in {
    val arg = javaIdentifier("name") ~? Literal(":") ~? javaIdentifier("type")
    val args  = Repsep(arg, Literal(","), None)//"args"
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
      scalaMethodHeader.matchPrefix(InputState(input)) match {
        case Right(PatternMatch(tn, matched, InputState2(`input`, _, _), _)) =>
        case Left(report) => fail(s"Failed to match input [$input]" + report)
        case _ => ???
      })
  }

  it should "match element content" in {
    val pattern = Literal("<title>") ~ Break(Literal("</title>"))

    val content = "this is a test"
    val formats = Seq(
      s"<title>$content</title>"
    )
    formats.map(input =>
      pattern.matchPrefix(InputState(input)) match {
        case Right(PatternMatch(tn, matched, InputState2(`input`, _, _), _)) =>
        //println(s"Successfully parsed [$matched]")
        case _ => ???
      })

  }

}

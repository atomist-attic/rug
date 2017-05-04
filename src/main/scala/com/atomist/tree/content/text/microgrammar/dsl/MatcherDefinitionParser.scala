package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.rug.BadRugException
import com.atomist.source.StringFileArtifact
import com.atomist.tree.content.text.microgrammar._
import com.atomist.tree.content.text.microgrammar.matchers.Break
import com.atomist.util.scalaparsing.CommonTypesParser

import scala.util.matching.{Regex => ScalaRegex}

/**
  * Parse our matcher DSL using a Scala parser combinator.
  */
class MatcherDefinitionParser extends CommonTypesParser {

  import MatcherDefinitionParser._

  // This parser does NOT skip whitespace, unlike most parser combinators.
  // So we need to override this value.
  override val skipWhitespace = false

  private def whitespaceSep: Parser[String] = """\s*""".r

  private def singleWordLiteral: Parser[Literal] =
    AnythingButReservedCharacters ^^ (l => Literal(l))

  private def rex: Parser[Regex] =
    RegexpOpenToken ~> anythingBut(Set(escape(RegexpCloseToken))) <~ RegexpCloseToken ^^ (r => Regex(r))

  private def matcherTerm: Parser[Matcher] =
    rex |
      singleWordLiteral |
      inlineReference

  private def concatenation: Parser[Matcher] =
    matcherTerm ~ opt(whitespaceSep) ~ matcherExpression ^^ {
      case left ~ _ ~ right =>
        //left ~? right
        Concat(Concat(left, Whitespace.?()), right)
    }

  private def variableName: Parser[String] = ident.filter(!_.contains(VariableDeclarationToken))

  // $name:[.*]
  private def inlineReference: Parser[Matcher] =
    VariableDeclarationToken ~> variableName ~ opt(":" ~ rex) ^^ {
      case newName ~ Some(_ ~ regex) => Wrap(regex, newName)
      case matcherName ~ None => Reference(matcherName)
    }

  private def matcherExpression: Parser[Matcher] =
    opt("""\s+""".r) ~ (concatenation | matcherTerm) ~ opt("""\s+""".r) ^^ {
      case Some(ws) ~ term ~ None => Concat(Whitespace.?(), term)
      case None ~ term ~ Some(ws) => Concat( term, Whitespace.?())
      case Some(ws) ~ term ~ Some(ws2) => Concat(Concat(Whitespace.?(), term), Whitespace.?())
      case None ~ term ~ None => term
    }

  /**
    * Parse the given microgrammar definition given a registry of known matchers.
    * Caller is responsible for updating the registry is they wish
    *
    * @param name       for the matcher
    * @param matcherDef definition
    * @return matcher definition
    */
  @throws[BadRugException]
  def parseMatcher(name: String, matcherDef: String): Matcher = matcherDef match {
    case null =>
      throw new BadRugException(s"The null string is not a valid microgrammar") {}
    case _ =>
      val m = parseTo(StringFileArtifact("<input>", matcherDef), phrase(matcherExpression))
      Wrap(m, name)
  }

  def parseAnonymous(matcherDef: String): Matcher = parseTo(StringFileArtifact("<input>", matcherDef), phrase(matcherExpression))

}

object MatcherDefinitionParser {

  val BreakOpenToken = "¡"
  val BreakCloseToken = "¡"
  val RegexpOpenToken = "§"
  val RegexpCloseToken = "§"
  val PredicateOpenToken = "["
  val PredicateCloseToken = "]"
  val VariableDeclarationToken = "$"

  private def escape(token: String) = """\""" + token

  def anythingBut(tokens: Set[String]): ScalaRegex =
    ("""[^""" + // NOT any of the following characters
      tokens.mkString("") +
      """]+""").r // at least one of any other character

  val AnythingButReservedCharacters: ScalaRegex =
    anythingBut(Set(
      """\s""", // whitespace
      escape(PredicateOpenToken),
      escape(PredicateCloseToken),
      escape(RegexpOpenToken),  // didn't include RegexpCloseToken because they're currently identical
      VariableDeclarationToken
    ))
}

private case class StatePredicateTest(name: String, value: String)
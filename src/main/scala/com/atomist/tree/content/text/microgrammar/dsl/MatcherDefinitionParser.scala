package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.rug.BadRugException
import com.atomist.source.StringFileArtifact
import com.atomist.tree.content.text.microgrammar._
import com.atomist.tree.content.text.microgrammar.matchers.Break
import com.atomist.util.scalaparsing.CommonTypesParser

/**
  * Parse our matcher DSL using a Scala parser combinator.
  */
class MatcherDefinitionParser extends CommonTypesParser {

  // This parser does NOT skip whitespace, unlike most parser combinators.
  // So we need to override this value.
  override protected val whiteSpace = "".r

  private def whitespaceSep: Parser[String] = """\s*""".r

  // TODO may want to consider whether we want to use $
  private def literal: Parser[Literal] =
    """[^▶\s\[\]\¡\§$]+""".r ^^ (l => Literal(l))

  private def rex(implicit matcherName: String): Parser[Regex] =
  //regex('[', ']')
    "§" ~> """[^\¡\§]+""".r <~ "§" ^^ (r => Regex(matcherName, r))

  /**
    * Skip till this clause
    * @return
    */
  private def break(implicit matcherName: String, registry: MatcherRegistry): Parser[Break] =
    "¡" ~> matcherExpression <~ "¡" ^^ (m => Break(m))

  private def predicateValue: Parser[String] = "true" | "false" | "\\d+".r

  // Applies to a boxed clause
  // [curlyDepth=1]
  private def predicate(implicit matcherName: String, registry: MatcherRegistry): Parser[StatePredicateTest] =
    "[" ~> ident ~ "=" ~ predicateValue <~ "]" ^^ {
    case predicateName ~ "=" ~ predicateVal => StatePredicateTest(predicateName, predicateVal)
  }

  private def boxedClause(implicit matcherName: String, registry: MatcherRegistry): Parser[Matcher] =
    "[" ~> matcherExpression <~ "]" ~ opt(predicate)

  // There is a deeper level to this game
  // The boxed clause should be a subnode of this node, rather than have its fields added directly
  private def descendantClause(implicit matcherName: String, registry: MatcherRegistry): Parser[Matcher] = "▶" ~> variableReference() ^^ (
    vr => Wrap(vr, vr.name)
    )

  private def matcherTerm(implicit matcherName: String, registry: MatcherRegistry): Parser[Matcher] =
    rex |
      literal |
      variableReference() |
      inlineReference()

  private def concatenation(implicit matcherName: String, registry: MatcherRegistry): Parser[Matcher] =
    matcherTerm ~ opt(whitespaceSep) ~ matcherExpression ^^ {
      case left ~ _ ~ right =>
        //left ~? right
        Concat(Concat(left, Whitespace.?), right, matcherName)
    }

  // TODO mixin that adds predicate check to a matcher

  // $name:Identifier
  private def variableReference()(implicit matcherName: String, registry: MatcherRegistry): Parser[Matcher] =
    "$" ~> opt(ident) ~ ":" ~ ident ~ opt(predicate) ^^ {
      case Some(name) ~ _ ~ kind ~ predicate =>
        registry
          .find(kind)
          .map(m => Reference(m, name))
          .getOrElse(throw new BadRugException(
            s"Cannot find referenced matcher of type [$kind] in $registry") {})
      case None ~ _ ~ kind ~ predicate =>
        registry
          .find(kind)
          .getOrElse(throw new BadRugException(s"Cannot find referenced matcher of type [$kind] in $registry") {})
    }

  // $name:[.*]
  private def inlineReference()(implicit matcherName: String, registry: MatcherRegistry): Parser[Matcher] =
    "$" ~> ident ~ ":" ~ rex ^^ {
      case newName ~ _ ~ regex => regex.copy(name = newName)
    }

  private def matcherExpression(implicit matcherName: String, registry: MatcherRegistry): Parser[Matcher] =
    descendantClause |
      concatenation |
      matcherTerm

  /**
    * Parse the given microgrammar definition given a registry of known matchers.
    * Caller is responsible for updating the registry is they wish
    *
    * @param name       for the matcher
    * @param matcherDef definition
    * @param mRegistry  known matchers
    * @return matcher definition
    */
  @throws[BadRugException]
  def parseMatcher(name: String, matcherDef: String, mRegistry: MatcherRegistry = EmptyMatcherRegistry): Matcher = matcherDef match {
    case null =>
      throw new BadRugException(s"The null string is not a valid microgrammar") {}
    case _ =>
      implicit val matcherName: String = name
      implicit val registry: MatcherRegistry = mRegistry
      val m = parseTo(StringFileArtifact("<input>", matcherDef), phrase(matcherExpression))
      m
  }

}

private case class StatePredicateTest(name: String, value: String)
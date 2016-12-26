package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.rug.BadRugException
import com.atomist.source.StringFileArtifact
import com.atomist.tree.content.text.microgrammar._
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
    """[^▶\s\[\]$]+""".r ^^ (l => Literal(l))

  private def rex: Parser[Regex] =
  //regex('[', ']')
    "§" ~> "[^\\§]+".r <~ "§" ^^ (r => Regex("name", r))

  private def predicateValue: Parser[String] = "true" | "false" | "\\d+".r

  // [curlyDepth=1]
  private def predicate(implicit registry: MatcherRegistry): Parser[StatePredicateTest] = "[" ~> ident ~ "=" ~ predicateValue <~ "]" ^^ {
    case predicateName ~ "=" ~ predicateVal => StatePredicateTest(predicateName, predicateVal)
  }

  private def boxedClause(implicit registry: MatcherRegistry): Parser[Matcher] = "[" ~> matcherExpression <~ "]" ~ opt(predicate)

  // There is a deeper level to this game
  // The boxed clause should be a subnode of this node, rather than have its fields added directly
  private def descendantClause(implicit registry: MatcherRegistry): Parser[Matcher] = "▶" ~> variableReference() ^^ (
    vr => Wrap(vr, vr.name)
    )

  private def matcherTerm(implicit registry: MatcherRegistry): Parser[Matcher] =
    rex |
      literal |
      variableReference() |
      inlineReference()

  private def concatenation(implicit registry: MatcherRegistry): Parser[Matcher] =
    matcherTerm ~ opt(whitespaceSep) ~ matcherExpression ^^ {
      case left ~ _ ~ right => left ~? right
    }

  // TODO mixin that adds predicate check to a matcher

  // $name:Identifier
  private def variableReference()(implicit registry: MatcherRegistry): Parser[Matcher] =
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
  private def inlineReference()(implicit registry: MatcherRegistry): Parser[Matcher] =
    "$" ~> ident ~ ":" ~ rex ^^ {
      case newName ~ _ ~ regex => regex.copy(name = newName)
    }

  private def matcherExpression()(implicit registry: MatcherRegistry): Parser[Matcher] =
    descendantClause |
      concatenation |
      matcherTerm

  private def microgrammar(implicit registry: MatcherRegistry): Parser[Matcher] = matcherExpression

  /**
    * Parse the given microgrammar definition given a registry of known matchers
    *
    * @param microgrammarDef definition
    * @param mRegistry       known matchers
    * @return matcher definition
    */
  @throws[BadRugException]
  def parse(microgrammarDef: String, mRegistry: MatcherRegistry = EmptyMatcherRegistry): Matcher = microgrammarDef match {
    case null =>
      throw new BadRugException(s"The null string is not a valid microgrammar") {}
    case _ =>
      implicit val registry: MatcherRegistry = mRegistry
      parseTo(StringFileArtifact("<input>", microgrammarDef), phrase(microgrammar))
  }

  /**
    * Parse several microgrammars, building a matcher and validating references.
    */
  @throws[BadRugException]
  def parseInOrder(microgrammarDefs: Seq[MicrogrammarDefinition], startingRegistry: MatcherRegistry = EmptyMatcherRegistry): MatcherRegistry = {
    val mmr = new MutableMatcherRegistry(startingRegistry)
    for {
      md <- microgrammarDefs
    } {
      val parsed = Reference(parse(md.sentence, mmr), md.name)
      mmr register parsed
    }
    mmr
  }

  private class MutableMatcherRegistry(start: MatcherRegistry) extends MatcherRegistry {
    private var matchers = start.definitions

    override def find(name: String): Option[Matcher] = matchers.find(_.name.equals(name))

    override def definitions: Seq[Matcher] = matchers

    def register(m: Matcher): Unit = matchers = matchers :+ m
  }

}

case class MicrogrammarDefinition(name: String, sentence: String)


private case class StatePredicateTest(name: String, value: String)
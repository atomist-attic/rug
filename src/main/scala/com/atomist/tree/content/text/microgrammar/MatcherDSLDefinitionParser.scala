package com.atomist.tree.content.text.microgrammar

import com.atomist.rug.BadRugException
import com.atomist.source.StringFileArtifact
import com.atomist.util.scalaparsing.CommonTypesParser

/**
  * Parse our matcher DSL.
  */
class MatcherDSLDefinitionParser extends CommonTypesParser {

  // This parser does NOT skip whitespace, unlike most parser combinators.
  // So we need to override this value.
  override protected val whiteSpace = "".r

  private def whitespaceSep = """\s+""".r

  private def literal: Parser[Literal] =
    """[^▶\s\[\]$]+""".r ^^ {
      case l => Literal(l)
    }

  // [curlyDepth=1]
  private def boxedClause(implicit registry: MatcherRegistry): Parser[Matcher] = "[" ~> matcherExpression <~ "]"

  private def descendantClause(implicit registry: MatcherRegistry): Parser[Matcher] = "▶" ~> boxedClause

  private def matcherTerm: Parser[Matcher] = literal

  private def concatenation(implicit registry: MatcherRegistry): Parser[Matcher] = matcherTerm ~ whitespaceSep ~ matcherExpression ^^ {
    case left ~ _ ~ right => left ~~ right
  }

  private def variableReference()(implicit registry: MatcherRegistry): Parser[Matcher] =
    "$" ~> opt(ident) ~ ":" ~ ident ^^ {
      case Some(name) ~ _ ~ kind =>
        registry
          .find(kind)
          .map(m => Reference(m, name))
          .getOrElse(throw new BadRugException(s"Cannot find referenced matcher of type [$kind]") {})
      case None ~ _ ~ kind =>
        registry
          .find(kind)
          .getOrElse(throw new BadRugException(s"Cannot find referenced matcher of type[$kind]") {})
    }

  private def matcherExpression()(implicit registry: MatcherRegistry): Parser[Matcher] = descendantClause | concatenation | literal | variableReference()

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

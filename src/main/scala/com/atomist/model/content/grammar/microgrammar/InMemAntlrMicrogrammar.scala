package com.atomist.model.content.grammar.microgrammar

import com.atomist.model.content.grammar.MatchListener
import com.atomist.model.content.grammar.antlr.{AbstractInMemAntlrGrammar, ModelBuildingListener, ParserSetup}
import com.atomist.model.content.text.{AbstractMutableContainerTreeNode, MutableContainerTreeNode}
import com.atomist.rug.RugRuntimeException
import com.atomist.scalaparsing.CommonTypesParser
import com.atomist.source.StringFileArtifact
import org.antlr.v4.runtime.InputMismatchException
import org.snt.inmemantlr.GenericParser

object InMemAntlrMicrogrammar {

  val WhitespaceLexerPattern = """[ \n\t\r]"""

  /**
    * What we can skip in pattern
    */
  val DefaultJunkLexerPattern = "~" + WhitespaceLexerPattern

  val UnwordProduction = "unword"

  val UnwordBinding = "junk"

  object LastLineParser extends CommonTypesParser {

    private def lastProduction: Parser[String] = ident <~ ":" <~ ".*".r

    def parse(lastLine: String): String = {
      parseTo(StringFileArtifact("<input>", lastLine), lastProduction)
    }
  }

  /**
    * Parse the grammar and take the last production. We need to do this
    * to know what we're trying to match (or not).
    *
    * @param grammar
    * @param junkLexerPattern
    * @return
    */
  def lastProduction(grammar: String, junkLexerPattern: String = DefaultJunkLexerPattern): Microgrammar = {
    val lastLine = grammar.lines.toSeq.reverse.find(_.contains(":")).getOrElse(
      throw new RugRuntimeException(null, s"Cannot parse grammar [$grammar] to extract last rule")
    )
    val matchProductionName = LastLineParser.parse(lastLine)
    new InMemAntlrMicrogrammar(grammar, matchProductionName, junkLexerPattern)
  }
}

import com.atomist.model.content.grammar.microgrammar.InMemAntlrMicrogrammar._

/**
  * Antlr microgrammar support.
  *
  * @param grammar Antlr G4 grammar definition to use
  * @param matchProductionName name of rule (production) defining a match
  * @param junkLexerPattern pattern to match junk without consuming too much input
  */
class InMemAntlrMicrogrammar(
                              grammar: String,
                              matchProductionName: String,
                              junkLexerPattern: String = InMemAntlrMicrogrammar.DefaultJunkLexerPattern)
  extends AbstractInMemAntlrGrammar
    with Microgrammar {

  private def maybeMatchProductionName(matchProduction: String) = s"${matchProduction}_or_not"

  private def matchesProductionName(matchProduction: String) = s"${matchProduction}_matches"

  private def looseMatchGrammar: String = looseGrammar(grammar, matchProductionName)

  override protected def setup = {
    val parser = new GenericParser(looseMatchGrammar, matchesProductionName(matchProductionName), this)
    // val parser = GenericParser.independentInstance(looseMatchGrammar, matchesProductionName(matchProductionName), this)
    ParserSetup(grammar, parser, matchProductionName)
  }

  /**
    * Create a grammar that can match multiple times in the same input
    *
    * @param g4 grammar fragment to use, including target production
    * @return a complete grammar matching the target production 0 or more times against the entire input
    */
  private def looseGrammar(g4: String, matchRuleName: String): String = {
    val matchesProduction = matchesProductionName(matchRuleName)
    val maybeMatchProduction = maybeMatchProductionName(matchRuleName)
    logger.debug(s"Creating loose match grammar from raw input======\n$g4\n=====\n")
    val prefix = if (g4.contains("grammar")) "" else s"grammar $matchesProduction;\n"
    val ignoreWhiteSpace =
      s"""
         |WHITESPACE: $WhitespaceLexerPattern;
         |WS: $WhitespaceLexerPattern+ -> skip;""".stripMargin
    prefix + ignoreWhiteSpace + g4 +
      s"""
         |JUNK: $junkLexerPattern;
         |$UnwordProduction : JUNK;
         |$maybeMatchProduction: found=$matchRuleName| $UnwordBinding=$UnwordProduction;
         |$matchesProduction: WHITESPACE* $maybeMatchProduction*;
        """.stripMargin
  }

  override def strictMatch(input: String, l: Option[MatchListener]): MutableContainerTreeNode = {
    val results = findMatches(input, l)
    require(results.size == 1, s"Expected 1 result, not ${results.size}")
    val r = results.head
    r
  }

  override def findMatches(input: String, ml: Option[MatchListener]): Seq[MutableContainerTreeNode] = {
    val topLevel = matchesProductionName(matchProductionName)
    logger.debug(s"Using grammar:\n$looseMatchGrammar")
    val l = new ModelBuildingListener(matchProductionName, ml)
    try {
      val parser = config.parser
      parser.setListener(l)
      parser.parse(input, topLevel)
    }
    catch {
      case ime: InputMismatchException =>
        logger.info(s"Antlr parse failure: $ime on [$input]", ime)
      // Return any results we found
    }

    l.results.foreach {
      case asu: AbstractMutableContainerTreeNode => asu.pad(input)
    }
    l.results
  }
}

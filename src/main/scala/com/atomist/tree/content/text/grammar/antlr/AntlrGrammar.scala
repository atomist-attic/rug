package com.atomist.tree.content.text.grammar.antlr

import com.atomist.tree.content.text.grammar.{MatchListener, Parser}
import com.atomist.tree.content.text.{AbstractMutableContainerTreeNode, MutableContainerTreeNode}
import org.antlr.v4.runtime.InputMismatchException
import org.snt.inmemantlr.GenericParser

/**
  * Parse input using Antlr, with classes created and compiled on the fly.
  * Uses a patched version of Inmemantlr (https://github.com/julianthome/inmemantlr)
  *
  * @param grammar Antlr G4 grammar definition to use
  */
class AntlrGrammar(
                    grammar: String,
                    production: String)
  extends AbstractInMemAntlrGrammar
    with Parser {

  override protected def setup = {
    // Extract the grammar name from the relevant line
    val grammarRe = "grammar ([A-Z][a-zA-Z0-9]*);.*".r
    val names = grammar.lines.flatMap {
      case grammarRe(name) =>
        Some(name)
      case l =>
        None
    }
    val name = names.toSeq.headOption.getOrElse(throw new IllegalArgumentException("Cannot match grammar name"))
    val parser = GenericParser.independentInstance(this, grammar)
    ParserSetup(grammar, parser, production)
  }

  override def parse(input: String, ml: Option[MatchListener]): MutableContainerTreeNode = {
    logger.debug(s"Using grammar:\n$grammar")
    val l = new ModelBuildingListener(production, ml)
    try {
      val parser = config.parser
      parser.setListener(l)
      parser.parse(input, production)
    }
    catch {
      case ime: InputMismatchException =>
        logger.info(s"Antlr parse failure: $ime on [$input]", ime)
      // Return any results we found
    }

    require(l.results.size == 1, s"Should have found 1 $production, not ${l.results.size}")

    val updatedResult = l.results.head
    updatedResult match {
      case asu: AbstractMutableContainerTreeNode => asu.pad(input, true)
    }
    updatedResult
  }
}

package com.atomist.tree.content.text.grammar.antlr

import com.atomist.tree.content.text.grammar.{MatchListener, Parser}
import com.atomist.tree.content.text.{AbstractMutableContainerTreeNode, MutableContainerTreeNode}
import org.antlr.v4.runtime.InputMismatchException
import org.snt.inmemantlr.GenericParser

/**
  * Parse input using Antlr, with classes created and compiled on the fly.
  * Uses Inmemantlr (https://github.com/julianthome/inmemantlr)
  *
  * @param grammars Antlr G4 grammar definitions to use
  */
class AntlrGrammar(
                    production: String,
                    grammars: String*)
  extends AbstractInMemAntlrGrammar
    with Parser {

  override protected def setup: ParserSetup = {
   //val parser = GenericParser.independentInstance(this, grammar)
    val parser = new GenericParser(this, false, grammars:_*)
    ParserSetup(grammars, parser, production)
  }

  override def parse(input: String, ml: Option[MatchListener]): Option[MutableContainerTreeNode] = {
    logger.debug(s"Using grammars:\n$grammars")
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

    val updatedResult = l.results.headOption
    updatedResult match {
      case Some(asu: AbstractMutableContainerTreeNode) => asu.pad(input, topLevel = true)
      case _ =>
    }
    updatedResult
  }
}

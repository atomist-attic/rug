package com.atomist.tree.content.text.grammar.antlr

import com.atomist.tree.content.text.grammar.{MatchListener, Parser}
import com.atomist.tree.content.text.{MutableContainerTreeNode, PositionedMutableContainerTreeNode, PositionedMutableContainerTreeNode$}
import org.antlr.v4.runtime.NoViableAltException
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
                    namingStrategy: AstNodeNamingStrategy,
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
    val l = new ModelBuildingListener(production, ml, namingStrategy)
    try {
      val parser = config.parser
      parser.setListener(l)
      parser.parse(input, production)
    }
    catch {
      case nva: NoViableAltException =>
        logger.info(s"Unable to parse file: $nva on [$input]", nva)
      case ime: InputMismatchException =>
        logger.info(s"Antlr parse failure: $ime on [$input]", ime)
      case t: Throwable =>
        logger.error(s"Unexpected Antlr parse failure: $t on [$input]", t)
      // Return any results we found
    }

    val updatedResult = l.ruleNodes.headOption
    updatedResult match {
      case Some(asu: PositionedMutableContainerTreeNode) => asu.pad(input, topLevel = true)
      case _ =>
    }
    updatedResult
  }
}

package com.atomist.tree.content.text.grammar.antlr

import com.atomist.tree.content.text.PositionedTreeNode
import org.antlr.v4.runtime.{InputMismatchException, NoViableAltException}
import org.snt.inmemantlr.GenericParser

/**
  * Parse input using Antlr, with classes created and compiled on the fly.
  * Uses Inmemantlr (https://github.com/julianthome/inmemantlr)
  *
  * @param grammars Antlr G4 grammar definitions to use
  */
class AntlrGrammar(
                    production: String,
                    nodeCreationStrategy: AstNodeCreationStrategy,
                    grammars: String*)
  extends AbstractInMemAntlrGrammar {

  override protected def setup: ParserSetup = {
    val parser = new GenericParser(this, false, grammars:_*)
    ParserSetup(grammars, parser, production)
  }

  def parse(input: String): Option[PositionedTreeNode] = {
    logger.debug(s"Using grammars:\n$grammars")
    val l = new ModelBuildingListener(production, nodeCreationStrategy)
    try {
      val parser = config.parser
      parser.setListener(l)
      parser.parse(input, production)
    } catch {
      case nva: NoViableAltException =>
        logger.info(s"Unable to parse file: $nva on [$input]", nva)
      case ime: InputMismatchException =>
        logger.info(s"Antlr parse failure: $ime on [$input]", ime)
      case t: Throwable =>
        logger.error(s"Unexpected Antlr parse failure: $t on [$input]", t)
      // Return any results we found
    }

    l.ruleNodes.headOption
  }
}

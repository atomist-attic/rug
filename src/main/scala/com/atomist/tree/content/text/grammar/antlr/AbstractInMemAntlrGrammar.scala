package com.atomist.tree.content.text.grammar.antlr

import com.atomist.rug.RugRuntimeException
import com.typesafe.scalalogging.LazyLogging
import org.antlr.v4.Tool
import org.antlr.v4.tool.{ANTLRMessage, ANTLRToolListener}
import org.snt.inmemantlr.GenericParser
import org.snt.inmemantlr.tool.ToolCustomizer
import org.stringtemplate.v4.ST

import scala.collection.mutable.ListBuffer

case class ParserSetup(
                        grammars: Seq[String],
                        parser: GenericParser,
                        production: String
                      )

/**
  * Convenient superclass for in memory use of Antlr grammars.
  * Parse input using Antrl, with classes created and compiled on the fly.
  * Uses Inmemantlr (https://github.com/julianthome/inmemantlr)
  */
abstract class AbstractInMemAntlrGrammar
  extends ToolCustomizer with LazyLogging {

  private val errorStore: ErrorStoringToolListener = new ErrorStoringToolListener

  /**
    *
    * @return Grammar and parser setup
    */
  protected def setup: ParserSetup

  private def compileGrammar(parser: GenericParser): Unit = {
    try {
      parser.compile()
    }
    catch {
      case t: Throwable =>
        logger.warn(s"Encountered Antlr exception ${t.getMessage}", t)
    }
    finally {
      if (errorStore.hasErrors)
        throw new RugRuntimeException(null, errorStore.toMessage, null)
    }
  }

  protected val config: ParserSetup = setup

  logger.debug(s"Compiling grammar-----\n$config\n-----")
  compileGrammar(config.parser)

  override def customize(tool: Tool): Unit = {
    errorStore.setTool(tool)
    tool.addListener(errorStore)
  }
}

@FunctionalInterface
trait ToolListenerCreator {

  def createListener(tool: Tool): ANTLRToolListener
}

class ErrorStoringToolListener extends ANTLRToolListener {

  private var tool: Tool = _

  private val _errors = new ListBuffer[String]

  def setTool(t: Tool): Unit = {
    this.tool = t
  }

  def errors: Seq[String] = _errors

  private def toSingleLineIfNecessary(msg: String) =
    if (tool.errMgr.formatWantsSingleLineMessage)
      msg.replace('\n', ' ')
    else msg

  override def info(msg: String) {
    val toShow = toSingleLineIfNecessary(msg)
  }

  override def error(msg: ANTLRMessage) {
    val msgST: ST = tool.errMgr.getMessageTemplate(msg)
    val outputMsg: String = msgST.render
    _errors.append(toSingleLineIfNecessary(outputMsg))
  }

  override def warning(msg: ANTLRMessage) {
    val msgST: ST = tool.errMgr.getMessageTemplate(msg)
    val outputMsg: String = msgST.render
  }

  def hasErrors: Boolean = _errors.nonEmpty

  def toMessage: String = {
    errors.mkString("\n")
  }
}
package com.atomist.tree.content.text.grammar

import com.atomist.tree.ContainerTreeNode
import com.atomist.tree.content.text.InputPosition
import com.atomist.util.ConsoleVisitor
import com.typesafe.scalalogging.LazyLogging

/**
  * Decorates a string with its position in input
  * @param value string
  * @param inputPosition position in input
  */
case class PositionalString(value: String, inputPosition: InputPosition)

/**
  * Invoked whenever a match is found. Useful for debugging or test infrastructure.
  */
trait MatchListener {

  /**
    * Return name identifying this listener.
    */
  def name: String

  def onSkip(junk: PositionalString): Unit

  def onMatch(m: ContainerTreeNode): Unit

  /**
    * Return the number of matches found.
    */
  def matches: Int
}

/**
  * Convenient superclass for implementing MatchListener.
  */
abstract class AbstractMatchListener(val name: String) extends MatchListener {

  private var _matches = 0

  override def onSkip(junk: PositionalString): Unit = {}

  final override def onMatch(m: ContainerTreeNode): Unit = {
    _matches += 1
    onMatchInternal(m)
  }

  protected def onMatchInternal(m: ContainerTreeNode): Unit

  final override def matches: Int = _matches
}

class ConsoleMatchListener(name: String, verbose: Boolean = false)
  extends AbstractMatchListener(name)
    with LazyLogging {

  override protected def onMatchInternal(m: ContainerTreeNode): Unit = {
    m.accept(ConsoleVisitor, 0)
  }

  override def onSkip(junk: PositionalString): Unit = {
    if (verbose)
      logger.info(s"Skipped [$junk]")
  }
}
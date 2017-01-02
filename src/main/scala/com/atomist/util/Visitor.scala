package com.atomist.util

/**
  * Simple visitor interface
  */
trait Visitor {

  /**
    * Boolean is whether this visit wants to visit the children of the current node, if any.
    */
  def visit(v: Visitable, depth: Int): Boolean
}

trait Visitable {

  def accept(v: Visitor, depth: Int)
}

/**
  * Visitor support that avoids cycles by tracking
  * the objects it has visited. Not for use on
  * huge graphs.
  */
trait CycleAvoidingVisitor extends Visitor {

  private var seen: Set[Visitable] = Set()

  final override def visit(v: Visitable, depth: Int): Boolean = {
    if (seen.contains(v))
      false
    else {
      seen += v
      visitInternal(v, depth)
    }
  }

  protected def visitInternal(v: Visitable, depth: Int): Boolean
}

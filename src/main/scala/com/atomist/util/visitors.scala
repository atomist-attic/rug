package com.atomist.util

import java.util.Objects

import scala.collection.mutable.ListBuffer

/**
  * Visitor implementation that saves all descendants. Do not use on huge trees!
  */
class SaveAllDescendantsVisitor extends CycleAvoidingVisitor {

  private val _nodes = ListBuffer.empty[Visitable]

  override def visitInternal(v: Visitable, depth: Int): Boolean = {
      _nodes.append(v)
      true
  }

  def descendants: Seq[Visitable] = _nodes
}


object ConsoleVisitor extends Visitor {

  override def visit(v: Visitable, depth: Int): Boolean = {
    v match {
      case _ =>
        println(List.fill(depth)("\t").mkString("") + Objects.toString(v))
    }
    true
  }
}

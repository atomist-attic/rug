package com.atomist.rug

import com.atomist.param.Parameter
import com.atomist.rug.parser._
import com.atomist.util.{Visitor, Visitable}

trait RugProgram extends Visitable {

  /**
    * If this is not specified, the operation isn't to be published to users
    *
    * @return
    */
  def publishedName: Option[String]

  def name: String

  def tags: Seq[String]

  def description: String

  def imports: Seq[Import]

  def parameters: Seq[Parameter]

  def computations: Seq[Computation]

  def actions: Seq[Action]

  def withs: Seq[With] = actions.collect {
    case w: With => w
  }

  /**
    * Return all the other operations that are be invoked from this program.
    * This enables the compiler to validate their presence and compatibility.
    *
    * @return
    */
  def runs: Seq[RunOtherOperation]

  override def accept(v: Visitor, depth: Int): Unit = {
    actions.foreach(a => a.accept(v, depth + 1))
  }
}

/**
  * Convenient superclass for programs that may invoke other programs.
  */
abstract class CanInvokeProjectOperation extends RugProgram {

  def runs: Seq[RunOtherOperation] = actions.collect {
    case roo: RunOtherOperation => roo
  }
}

/**
  * Represents a predicate that acts on a project and returns true/false.
  */
case class RugProjectPredicate(
                                name: String,
                                publishedName: Option[String],
                                tags: Seq[String],
                                description: String,
                                imports: Seq[Import],
                                parameters: Seq[Parameter],
                                computations: Seq[Computation],
                                actions: Seq[Action]
                              )
  extends RugProgram {

  override def runs: Seq[RunOtherOperation] = Nil

}

/**
  * Represents a parsed Rug editor. We can evaluate it.
  */
case class RugEditor(
                      name: String,
                      publishedName: Option[String],
                      tags: Seq[String],
                      description: String,
                      imports: Seq[Import],
                      preconditions: Seq[Condition],
                      postcondition: Option[Condition],
                      parameters: Seq[Parameter],
                      computations: Seq[Computation],
                      actions: Seq[Action]
                    )
  extends CanInvokeProjectOperation

case class RugReviewer(
                        name: String,
                        publishedName: Option[String],
                        tags: Seq[String],
                        description: String,
                        imports: Seq[Import],
                        parameters: Seq[Parameter],
                        computations: Seq[Computation],
                        actions: Seq[Action]
                      )
  extends CanInvokeProjectOperation

case class RugExecutor(
                        name: String,
                        publishedName: Option[String],
                        tags: Seq[String],
                        description: String,
                        imports: Seq[Import],
                        parameters: Seq[Parameter],
                        computations: Seq[Computation],
                        actions: Seq[Action]
                      )
  extends RugProgram {

  /**
    * Find all other operations that may be run requires drilling into all With blocks
    * to check if they call other operations. For other operation types it's not possible
    * to invoke other operations inside With blocks.
    *
    * @return
    */
  def runs: Seq[RunOtherOperation] = {

    def roosFor(action: Action): Seq[RunOtherOperation] = action match {
      case roo: RunOtherOperation => Seq(roo)
      case w: With => w.doSteps.flatMap(d => roosFor(d))
      case _ => Nil
    }
    actions.flatMap(a => roosFor(a))
  }
}

case class Condition(predicateOrReviewerName: String)

case class Import(fqn: String) {

  /**
    *
    * @return the name of the operation itself (without namespace qualification),
    *         analogous to "simple name" in Java
    */
  def simpleName: String = {
    val pathElements = fqn.split("\\.")
    pathElements.last
  }
}
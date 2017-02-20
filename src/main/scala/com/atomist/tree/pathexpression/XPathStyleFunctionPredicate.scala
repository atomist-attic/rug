package com.atomist.tree.pathexpression

import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.ExpressionEngine.NodePreparer
import XPathTypes._
import com.atomist.graph.GraphNode
import com.atomist.tree.utils.NodeUtils

/**
  * Handles an XPath predicate based on a function call
  *
  * @param name function
  * @param args arguments to the function
  */
case class XPathStyleFunctionPredicate(override val name: String,
                                       args: Seq[FunctionArg],
                                       functionRegistry: FunctionRegistry = DefaultFunctionRegistry)
  extends Predicate {

  val fun: Function = functionRegistry.find(name, args).getOrElse(
    throw new IllegalArgumentException(s"No function named [$name] with args [$args]: Function registry=\n$functionRegistry")
  )

  def evaluate(tn: GraphNode,
               among: Seq[GraphNode],
               ee: ExpressionEngine,
               typeRegistry: TypeRegistry,
               nodePreparer: Option[NodePreparer]): Boolean = {
    //println(s"Evaluate $this against $tn")
    val convertedArgs: Seq[Any] = args.zipWithIndex.map(tup =>
      convertToRequiredType(tup._1, fun.argTypes(tup._2), tn, ee, typeRegistry, nodePreparer))
    fun.invoke(convertedArgs) == true
  }

  private def convertToRequiredType(fa: FunctionArg, requiredType: XPathType,
                                    contextNode: GraphNode, ee: ExpressionEngine,
                                    tr: TypeRegistry, np: Option[NodePreparer]): Any =
    requiredType match {
      case String => convertToString(fa, contextNode, ee, tr, np)
      case Boolean => convertToBoolean(fa, contextNode, ee, tr, np)
      case _ => ???
    }

  // See XPath spec 4.2
  private def convertToString(fa: FunctionArg, contextNode: GraphNode, ee: ExpressionEngine, tr: TypeRegistry, np: Option[NodePreparer]): String = {
    val value = fa match {
      case s: StringLiteralFunctionArg => s.s
      case rpe: RelativePathFunctionArg => ee.evaluate(contextNode, rpe.pe, tr, np) match {
        case Right(l) if l.nonEmpty =>
          NodeUtils.value(l.head)
        case _ => ""
      }
    }
    //println(s"convertToString found node: $fa, value=[$value] against $contextNode")
    value
  }

  private def convertToBoolean(fa: FunctionArg, contextNode: GraphNode, ee: ExpressionEngine, tr: TypeRegistry, np: Option[NodePreparer]): Boolean = fa match {
    case s: StringLiteralFunctionArg => s.s == "true"
    case rpe: RelativePathFunctionArg => ee.evaluate(contextNode, rpe.pe, tr, np) match {
      case Right(l) if l.nonEmpty => true
      case _ => false
    }
  }

}

/**
  * Represents an XPath style function.
  */
trait Function {

  def name: String

  def argTypes: Seq[XPathType]

  def invoke(convertedArgs: Seq[Any]): Any
}

/**
  * Registry of known XPath style functions.
  */
trait FunctionRegistry {

  /**
    * Find a function with the given name that will take these args
    */
  def find(name: String, args: Seq[FunctionArg]): Option[Function] =
  functions.find(f => f.name == name)

  def functions: Seq[Function]

  override def toString: String = {
    s"${getClass.getName}\n${functions.map(f => f.name).sorted.mkString("\n")}"
  }

}

object DefaultFunctionRegistry
  extends ReflectiveFunctionRegistry(StandardFunctions)

package com.atomist.tree.pathexpression

import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.ExpressionEngine.NodePreparer

object XPathTypes extends Enumeration {

  type XPathType = Value

  val String, Boolean = Value
}

import XPathTypes._

/**
  * Handles an XPath predicate
  *
  * @param name function
  * @param args arguments to the function
  */
case class XPathStyleFunctionPredicate(override val name: String,
                                       args: Seq[FunctionArg])
  extends Predicate {

  val fun: Function = DefaultFunctionRegistry.find(name, args).getOrElse(
    throw new IllegalArgumentException(s"No function named [$name] with args [$args]")
  )

  def evaluate(tn: TreeNode,
               among: Seq[TreeNode],
               ee: ExpressionEngine,
               typeRegistry: TypeRegistry,
               nodePreparer: Option[NodePreparer]): Boolean = {
    val convertedArgs: Seq[Any] = args.zipWithIndex.map(tup =>
      convertToRequiredType(tup._1, fun.argTypes(tup._2), tn, ee, typeRegistry, nodePreparer))
    fun.invoke(convertedArgs) == true
  }

  private def convertToRequiredType(fa: FunctionArg, requiredType: XPathType, contextNode: TreeNode, ee: ExpressionEngine,
                                    tr: TypeRegistry, np: Option[NodePreparer]): Any =
    requiredType match {
      case String => convertToString(fa, contextNode, ee, tr, np)
      case Boolean => convertToBoolean(fa, contextNode, ee, tr, np)
      case _ => ???
    }

  // See XPath spec 4.2
  private def convertToString(fa: FunctionArg, contextNode: TreeNode, ee: ExpressionEngine, tr: TypeRegistry, np: Option[NodePreparer]): String = fa match {
    case s: StringLiteralFunctionArg => s.s
    case rpe: RelativePathFunctionArg => ee.evaluate(contextNode, rpe.pe, tr, np) match {
      case Right(l) if l.nonEmpty => l.head.value
      case _ => ""
    }
  }

  private def convertToBoolean(fa: FunctionArg, contextNode: TreeNode, ee: ExpressionEngine, tr: TypeRegistry, np: Option[NodePreparer]): Boolean = fa match {
    case s: StringLiteralFunctionArg => s.s == "true"
    case rpe: RelativePathFunctionArg => ee.evaluate(contextNode, rpe.pe, tr, np) match {
      case Right(l) if l.nonEmpty => true
      case _ => false
    }
  }

}

trait Function {

  def name: String

  def argTypes: Seq[XPathType]

  def invoke(convertedArgs: Seq[Any]): Any
}

trait FunctionRegistry {

  /**
    * Find a function with the given name that will take these args
    */
  def find(name: String, args: Seq[FunctionArg]): Option[Function]

}

object DefaultFunctionRegistry extends FunctionRegistry {

  private val functions: Seq[Function] = Seq(
    SimpleFunction("contains", Seq(String, String), args => {
      if (args(0) == null || args(1) == null) false
      else args(0).toString.contains(args(1).toString)
    })
  )

  override def find(name: String, args: Seq[FunctionArg]): Option[Function] = functions.find(f => f.name == name)

}

/**
  * Note that we don't need to write defensive code here as there will be the correct number of arguments
  * and they will have been coerced as to type
  */
private case class SimpleFunction(name: String, argTypes: Seq[XPathType], invoker: Seq[Any] => Any) extends Function {

  override def invoke(convertedArgs: Seq[Any]): Any = {
    require(convertedArgs.size == argTypes.size)
    invoker(convertedArgs)
  }
}

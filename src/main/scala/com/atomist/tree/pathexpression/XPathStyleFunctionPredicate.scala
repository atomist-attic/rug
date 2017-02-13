package com.atomist.tree.pathexpression

import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.ExpressionEngine.NodePreparer

/**
  * Handles an XPath predicate
  * @param name function
  * @param args arguments to the function
  */
case class XPathStyleFunctionPredicate(override val name: String,
                                       args: Seq[FunctionArg])
  extends Predicate {

  def evaluate(tn: TreeNode,
               among: Seq[TreeNode],
               ee: ExpressionEngine,
               typeRegistry: TypeRegistry,
               nodePreparer: Option[NodePreparer]): Boolean = {
    println(this)
    ???
  }

}

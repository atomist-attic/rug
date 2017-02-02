package com.atomist.rug.kind.grammar

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.FileArtifactBackedMutableView
import com.atomist.rug.kind.dynamic.MutableContainerMutableView
import com.atomist.rug.spi.Typed
import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpression}
import com.atomist.tree.{MutableTreeNode, TreeNode}

/**
  * Convenient superclass for top level types based on a raw node under File,
  * using path expressions.
  * Type name is significant, and taken from class name, stripping the
  * "MutableView" suffix
  *
  * @param topLevelNode underlying parsed node
  * @param f file the node was parsed from
  */
abstract class RawNodeUnderFileMutableView(topLevelNode: MutableContainerTreeNode, f: FileArtifactBackedMutableView)
  extends MutableContainerMutableView(topLevelNode, f) {

  protected val expressionEngine: ExpressionEngine = f.parent.context.pathExpressionEngine.ee

  /**
    * The name of this file is significant
    *
    * @return the type of the node.
    */
  override def nodeTags: Set[String] = Set(Typed.typeToTypeName(getClass))

  /**
    * Convenient method to operate on nodes selected by a path expression
    * @param pexpr Path expression under this node
    * @param f     function on TreeNode
    */
  protected def doWithNodesMatchingPath(pexpr: PathExpression, f: MutableTreeNode => Unit): Unit = {
    expressionEngine.evaluate(topLevelNode, pexpr, DefaultTypeRegistry) match {
      case Right(nodes: Seq[TreeNode]) =>
        for (n <- nodes) {
          n match {
            case mtn: MutableTreeNode =>
              //println(s"Matched mutable node '${mtn.value}'")
              f(mtn)
              //println(s"Changed value to '${mtn.value}'")
          }
        }
      case Left(_) => ???
    }
  }

}

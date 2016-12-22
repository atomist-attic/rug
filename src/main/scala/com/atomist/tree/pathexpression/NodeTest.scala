package com.atomist.tree.pathexpression

import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.content.text._
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult
import com.atomist.tree.{ContainerTreeNode, TreeNode}

/**
  * One of the three core elements of a LocationStep. Inspired by XPath NodeTest.
  */
trait NodeTest {

  /**
    * Find nodes from the given node, observing the given AxisSpecifier
    * @param tn node to drill down from
    * @param axis AxisSpecifier indicating the kind of navigation
    * @return Resulting nodes
    */
  def follow(tn: TreeNode, axis: AxisSpecifier, typeRegistry: TypeRegistry): ExecutionResult

}

/**
  * Convenience superclass that uses a predicate to handle sourced nodes
  *
  * @param predicate predicate used to filter nodes
  */
abstract class PredicatedNodeTest(name: String, predicate: Predicate) extends NodeTest {

  final override def follow(tn: TreeNode, axis: AxisSpecifier, typeRegistry: TypeRegistry): ExecutionResult =
    sourceNodes(tn, axis, typeRegistry) match {
    case Right(nodes) =>
      Right(nodes.filter(tn => predicate(tn, nodes)))
    case failure => failure
  }

  /**
    * Subclasses can override this to provide a more efficient implementation.
    * This one works but can be expensive.
    */
  protected def sourceNodes(tn: TreeNode, axis: AxisSpecifier, typeRegistry: TypeRegistry): ExecutionResult = axis match {
    case Self => Right(List(tn))
    case Child => tn match {
      case ctn: ContainerTreeNode =>
        val kids = ctn.childNodes.toList
        Right(kids)
      case x => ExecutionResult.empty
    }
    case Descendant =>
      val kids = Descendant.allDescendants(tn).toList
      Right(kids)
  }
}

/**
  * Return all nodes on the given axis
  */
object All extends PredicatedNodeTest("All", TruePredicate)

case class NamedNodeTest(name: String)
  extends NodeTest {

  override def follow(tn: TreeNode, axis: AxisSpecifier, typeRegistry: TypeRegistry): ExecutionResult = axis match {
    case Child =>
      tn match {
        case ctn: ContainerTreeNode =>
          val kids: List[TreeNode] =
            ctn.apply(name).toList match {
              case Nil =>
                TreeNodeOperations.invokeMethodIfPresent[TreeNode](tn, name).
                  map {
                    case s: List[TreeNode@unchecked] =>
                      s
                    case t: TreeNode =>
                      List(t)
                    case x =>
                      Nil
                  }.getOrElse {
                  val allKids = ctn.childNodes
                  val filteredKids = allKids.filter(n => name.equals(n.nodeName))
                  filteredKids.toList
                }
              case l => l
            }
          Right(kids)
        case x => Left(s"Cannot find property [$name] on non-container tree node [$x]")
      }
  }

}
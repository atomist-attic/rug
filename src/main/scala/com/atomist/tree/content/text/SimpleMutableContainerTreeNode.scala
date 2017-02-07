package com.atomist.tree.content.text

import com.atomist.tree.TreeNode
import com.atomist.tree.TreeNode.{Noise, Signal, Significance, Undeclared}

class SimpleMutableContainerTreeNode(
                                      name: String,
                                      val initialFieldValues: Seq[TreeNode],
                                      val startPosition: InputPosition,
                                      val endPosition: InputPosition,
                                      override val significance: Significance = TreeNode.Noise,
                                      val additionalTypes: Set[String] = Set())
  extends PositionedMutableContainerTreeNode(name) {

  additionalTypes.foreach(addType)

  initialFieldValues.foreach(insertFieldCheckingPosition)

  override def toString: String = {
      val sig = significance match {
        case Noise => "noise "
        case Signal => "signal "
        case Undeclared => ""
      }
    val description =
      if (padded)
        s"value=$value"
      else
        s"startPositioned=$startPosition"
      s"[${sig}container:name='$nodeName'; $description; children[${childNodes.mkString("\n")}]"
  }

}

object SimpleMutableContainerTreeNode {

  import OffsetInputPosition._

  /**
    * Create a top level  node for the entire input, with the given child nodes
    * that represent content parsed from the input. Creating padding nodes for
    * parts of the input that the fieldValues passed in don't explain
    * @param name name for the new node
    * @param fieldValues fieldValues parsed from input
    * @param input input string
    * @return a new node explaining the whole input
    */
  def wholeInput(name: String, fieldValues: Seq[TreeNode], input: String): SimpleMutableContainerTreeNode = {
    val moo = new SimpleMutableContainerTreeNode(name, fieldValues, startOf(input), endOf(input))
    moo.pad(input, topLevel = true)
    moo
  }

  /**
    * Wrap the given node in a higher level container node with the given name
    * @param name name for the new top level node
    * @param kids nodes to wrap
    * @return wrapper node containing the single child
    */
  def wrap(name: String, kids: Seq[PositionedTreeNode], significance: Significance = TreeNode.Noise): SimpleMutableContainerTreeNode = {
    require(kids.nonEmpty, "Must have children to wrap")
    val moo = new SimpleMutableContainerTreeNode(name, kids, kids.head.startPosition, kids.last.endPosition, significance)
    moo
  }

  /**
    * Wrap the given node in a higher level container node with the given name
    * @param name name for the new top level node
    * @param tn node to wrap
    * @return wrapper node containing the single child
    */
  def wrap(name: String, tn: PositionedTreeNode): SimpleMutableContainerTreeNode =
    wrap(name, Seq(tn), significance = TreeNode.Signal) // we wouldn't be wrapping one node without a reason


  /**
    * Create a padded, mutable tree from the given tree and complete input.
    * Ignore non PositionedTreeNode descendants.
    * Note that updating nodes will lose any structure below them, as the node
    * will be replaced with plain text.
    */
  def makeMutable(ptn: PositionedTreeNode, input: String): SimpleMutableContainerTreeNode = {
    val mut = makeMutable(ptn)
    mut.pad(input, topLevel = true)
    mut
  }

  /**
    * Convert this tree to a mutable tree. Ignore non PositionedTreeNode descendants.
    * Returns nodes that will need padding
    */
  private def makeMutable(ptn: PositionedTreeNode): SimpleMutableContainerTreeNode = {
    val kids = ptn.childNodes collect {
      case ptn: PositionedTreeNode =>
        makeMutable(ptn)
    }
    new SimpleMutableContainerTreeNode(ptn.nodeName, kids,
      ptn.startPosition, ptn.endPosition,
      significance = TreeNode.Signal,
      additionalTypes = ptn.nodeTags)
  }

}

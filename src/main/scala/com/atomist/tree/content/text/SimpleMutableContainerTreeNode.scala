package com.atomist.tree.content.text

import com.atomist.tree.TreeNode

class SimpleMutableContainerTreeNode(
                                      name: String,
                                      val initialFieldValues: Seq[TreeNode],
                                      val startPosition: InputPosition,
                                      val endPosition: InputPosition
                                    )
  extends AbstractMutableContainerTreeNode(name) {

  initialFieldValues.foreach(insertFieldCheckingPosition)

  override def childrenNamed(key: String): Seq[TreeNode] = fieldValues.filter(n => n.nodeName.equals(key))

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
  def wholeInput(name: String, fieldValues: Seq[TreeNode], input: String): MutableContainerTreeNode = {
    val moo = new SimpleMutableContainerTreeNode(name, fieldValues, startOf(input), endOf(input))
    moo.pad(input, true)
    moo
  }

  /**
    * Wrap the given node in a higher level container node with the given name
    * @param name name for the new top level node
    * @param kids nodes to wrap
    * @return wrapper node containing the single child
    */
  def wrap(name: String, kids: Seq[PositionedTreeNode]): SimpleMutableContainerTreeNode = {
    require(kids.nonEmpty, "Must have children to wrap")
    val moo = new SimpleMutableContainerTreeNode(name, kids, kids.head.startPosition, kids.last.endPosition)
    moo
  }

  /**
    * Wrap the given node in a higher level container node with the given name
    * @param name name for the new top level node
    * @param tn node to wrap
    * @return wrapper node containing the single child
    */
  def wrap(name: String, tn: PositionedTreeNode): SimpleMutableContainerTreeNode =
    wrap(name, Seq(tn))
}

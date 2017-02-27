package com.atomist.tree.content.text

import com.atomist.tree.TreeNode

/**
  * For usage by Scala ParserCombinators and other technologies where position information isn't availalble
  * until after node construction. Position information can be updated by parser after construction,
  * as in Scala parsing Positional. Fields can be added in subclass constructors or via superclass
  * methods.
  *
  * @param nodeName name of the node
  */
class ParsedMutableContainerTreeNode(nodeName: String)
  extends PositionedMutableContainerTreeNode(nodeName) {

  var startPosition: InputPosition = _

  var endPosition: InputPosition = _

  override def childrenNamed(key: String): Seq[TreeNode] = fieldValues.filter(n => n.nodeName == key)

}

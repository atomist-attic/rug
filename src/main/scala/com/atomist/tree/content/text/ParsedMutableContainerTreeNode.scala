package com.atomist.tree.content.text

import com.atomist.tree.TreeNode

/**
  * For usage by Scala ParserCombinators and others technologies where position information isn't availabe
  * until after node construction. Position information can be updated by parser after construction,
  * as in Scala parsing Positional. Fields can be added in subclass constructors or via superclass
  * methods.
  *
  * @param nodeName name
  */
class ParsedMutableContainerTreeNode(nodeName: String)
  extends PositionedMutableContainerTreeNode(nodeName) {

  var startPosition: InputPosition = _

  var endPosition: InputPosition = _

  override def childrenNamed(key: String): Seq[TreeNode] = fieldValues.filter(n => n.nodeName.equals(key))

}

package com.atomist.model.content.text

/**
  * For usage by Scala ParserCombinators and others technologies where position information isn't availabe
  * until after node construction. Position information can be updated by parser after construction,
  * as in Scala parsing Positional. Fields can be added in subclass constructors or via superclass
  * methods.
  *
  * @param nodeName name
  */
class ParsedMutableContainerTreeNode(nodeName: String)
  extends AbstractMutableContainerTreeNode(nodeName) {

  var startPosition: InputPosition = _

  var endPosition: InputPosition = _
}

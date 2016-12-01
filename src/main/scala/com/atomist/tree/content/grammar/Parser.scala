package com.atomist.tree.content.grammar

import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.utils.TreeNodeOperations.{NodeTransformer, TreeOperation}

/**
  * Interface for parsers that can parse input into a MutableContainerTreeNode,
  * ready to use and modify.
  */
trait Parser {

  /**
    * Parse input
    * @param input input
    * @param ml optional listener that will be notified of matches
    * @return a parsed tree structure
    */
  def parse(input: String, ml: Option[MatchListener]): MutableContainerTreeNode

}

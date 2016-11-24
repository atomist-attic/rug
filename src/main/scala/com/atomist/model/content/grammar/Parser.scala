package com.atomist.model.content.grammar

import com.atomist.model.content.text.MutableContainerTreeNode
import com.atomist.model.content.text.TreeNodeOperations.{NodeTransformer, TreeOperation}

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

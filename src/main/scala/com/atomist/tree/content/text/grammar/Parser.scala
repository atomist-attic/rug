package com.atomist.tree.content.text.grammar

import com.atomist.tree.TreeNode

/**
  * Interface for parsers that can parse input into a MutableContainerTreeNode,
  * ready to use and modify.
  */
trait Parser {

  /**
    * Parse input if possible.
    * @param input input input to parse
    * @param ml optional listener that will be notified of matches
    * @return a parsed tree structure if input was valid. Otherwise None
    */
  def parse(input: String, ml: Option[MatchListener]): Option[TreeNode]

}

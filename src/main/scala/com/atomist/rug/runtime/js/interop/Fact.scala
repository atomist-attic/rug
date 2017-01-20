package com.atomist.rug.runtime.js.interop

import com.atomist.tree.TreeNode

trait Fact {

  val teamId: String

}


/**
  * Clear any relationships of this kind and replace them
  * with the given relationships
  * @param teamId
  * @param relationshipType
  */
case class Relationships(teamId: String,
                         source: TreeNode,
                         targets: Set[TreeNode],
                         relationshipType: String)
  extends Fact



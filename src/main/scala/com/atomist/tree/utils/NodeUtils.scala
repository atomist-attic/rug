package com.atomist.tree.utils

import com.atomist.graph.GraphNode
import com.atomist.tree.TreeNode

object NodeUtils {

  def value(gn: GraphNode): String = gn match {
    case tn: TreeNode => tn.value
    case _ => ""
  }
}

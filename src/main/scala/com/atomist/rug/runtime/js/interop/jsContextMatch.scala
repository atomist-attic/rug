package com.atomist.rug.runtime.js.interop

import com.atomist.graph.GraphNode

/**
  * Fronts JavaScript Context object passed to an EventHandler
  * Detyped as a Nashorn objects may be passed that do not implement GraphNode
  */
case class jsContextMatch(root: GraphNode,
                          matches: Seq[GraphNode],
                          pathExpressionEngine: jsPathExpressionEngine,
                          contextRoot: AnyRef,
                          teamId: String) {

  override def toString: String =
    s"ContextMatch: TeamId=[$teamId]; root=$root, matches=[${matches.mkString(",")}]"
}

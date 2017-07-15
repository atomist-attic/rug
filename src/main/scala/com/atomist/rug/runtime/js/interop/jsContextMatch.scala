package com.atomist.rug.runtime.js.interop

import com.atomist.graph.GraphNode
import com.atomist.rug.spi.ExportFunction

import scala.annotation.meta.getter

/**
  * Fronts JavaScript Context object passed to an EventHandler
  * Detyped as a Nashorn objects may be passed that do not implement GraphNode
  */
case class jsContextMatch(@(ExportFunction @getter)(description = "Root node of query", readOnly = true, exposeAsProperty = true)
                           root: GraphNode,
                          @(ExportFunction @getter)(description = "Query matches", readOnly = true, exposeAsProperty = true)
                          matches: Seq[GraphNode],
                          @(ExportFunction @getter)(description = "The Path Expression Engine", readOnly = true, exposeAsProperty = true)
                          pathExpressionEngine: jsPathExpressionEngine,
                          @(ExportFunction @getter)(description = "The root context for this team", readOnly = true, exposeAsProperty = true)
                          contextRoot: AnyRef,
                          @(ExportFunction @getter)(description = "Current team's id", readOnly = true, exposeAsProperty = true)
                          teamId: String) {

  override def toString: String =
    s"ContextMatch: TeamId=[$teamId]; root=$root, matches=[${matches.mkString(",")}]"
}

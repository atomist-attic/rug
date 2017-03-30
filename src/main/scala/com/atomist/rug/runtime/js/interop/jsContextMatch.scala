package com.atomist.rug.runtime.js.interop

/**
  * Fronts JavaScript Context object
  * Detyped as a Nashorn objects may be passed that do not implement GraphNode
  */
case class jsContextMatch(root: AnyRef,
                          matches: _root_.java.util.List[AnyRef],
                          teamId: String) {

  import scala.collection.JavaConverters._

  override def toString: String =
    s"ContextMatch: TeamId=[$teamId]; root=$root, matches=[${matches.asScala.mkString(",")}]"
}

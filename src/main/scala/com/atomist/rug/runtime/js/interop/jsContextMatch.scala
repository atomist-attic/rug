package com.atomist.rug.runtime.js.interop

/**
  * Fronts JavaScript Context object
  */
case class jsContextMatch(root: Object,
                          matches: _root_.java.util.List[jsSafeCommittingProxy],
                          teamId: String) {

  import scala.collection.JavaConverters._

  override def toString: String =
    s"ContextMatch: TeamId=[$teamId]; root=$root, matches=[${matches.asScala.mkString(",")}]"
}

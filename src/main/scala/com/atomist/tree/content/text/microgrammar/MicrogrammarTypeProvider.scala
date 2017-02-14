package com.atomist.tree.content.text.microgrammar

import com.atomist.rug.kind.core.{FileArtifactBackedMutableView, FileType}
import com.atomist.rug.kind.dynamic.{ChildResolver, MutableContainerMutableView, MutableTreeNodeUpdater}
import com.atomist.rug.spi.{TypeProvider, Typed}
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text._
import com.atomist.tree.content.text.grammar.MatchListener

/**
  * Type information for the results of evaluating a microgrammar.
  * It will have the name of the microgrammar, and can be
  * evaluated against any file
  *
  * @param microgrammar microgrammar to evaluate
  */
class MicrogrammarTypeProvider(microgrammar: Microgrammar)
  extends TypeProvider(classOf[MutableContainerMutableView])
    with ChildResolver {

  override val name: String = microgrammar.name

  override def description: String = s"Microgrammar type for [$name]"

  /**
    * Microgrammars can only be resolved from under files
    */
  override def findAllIn(context: TreeNode): Option[Seq[TreeNode]] = context match {
    case f: FileArtifactBackedMutableView =>
      val l: Option[MatchListener] = None
      val matches: Seq[PositionedTreeNode] = microgrammar.findMatches(f.content, l)
      Some(TextTreeNodeLifecycle.makeReady(microgrammar.name, matches, f))

    case _ => None
  }
}

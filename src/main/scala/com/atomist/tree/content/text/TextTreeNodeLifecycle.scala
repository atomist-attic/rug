package com.atomist.tree.content.text

import com.atomist.rug.kind.core.FileArtifactBackedMutableView
import com.atomist.tree.UpdatableTreeNode

object TextTreeNodeLifecycle {

  /**
    * This is how tree nodes progress:
    *
               ┌───────────────────┐                    ┌───────────────────┐
               │                   │                    │                   │
───parsing────▶│PositionedTreeNode │─────makeReady─────▶│ UpdatableTreeNode │───────▶  proceed with
               │                   │                    │                   │            updates
               └───────────────────┘                    └───────────────────┘
    *
    *  Parsing produces PositionedTreeNodes, which know their offset.
    *  makeReady incorporates the original contents of the file, and also the file's MutableView,
    *  so that the output UpdatableTreeNodes can report their value, accept a new value,
    *  and propogate that value all the way back to the file.
    */

  /* This is for Antlr grammars, or anything that represents the entire file in a single parsed node. */
  def makeWholeFileNodeReady(typeName: String, parsed: PositionedTreeNode, fileArtifact: FileArtifactBackedMutableView): UpdatableTreeNode = {
    val parsedWithWholeFileOffsets =
      ImmutablePositionedTreeNode(parsed).copy(
        startPosition = OffsetInputPosition(0),
        endPosition = OffsetInputPosition(fileArtifact.content.length))
    makeReady(typeName, Seq(parsedWithWholeFileOffsets), fileArtifact).head
  }

  /**
    * Some or all of the file has been parsed into some PositionedTreeNodes. (Microgrammars parse some. Antlr grammars parse all)
    *
    * Before we can update any of the nodes or their children, we have to:
    *
    * Wrap these in one node that always contains the entire content of the file; fill
    * any unrepresented characters with padding.
    * Transform the PositionedTreeNodes into UpdatableTreeNodes.
    * And link that top-level node to the file, so that updates to any node propagate to the file.
    * Cascade parentage information down into all the nodes.
    * Return the new, Updatable representations of the input PositionedTreeNodes.
    */
  def makeReady(typeName: String, matches: Seq[PositionedTreeNode], fileArtifact: FileArtifactBackedMutableView): Seq[UpdatableTreeNode] = {
    val wrapperNodeContainingWholeFileContent = ImmutablePositionedTreeNode.pad(typeName: String, matches, fileArtifact.content)
    wrapperNodeContainingWholeFileContent.setParent(fileArtifact)
    wrapperNodeContainingWholeFileContent.childNodes.collect {
      case utn: UpdatableTreeNode => utn // should be all of them
      case _ => ???
    }
  }
}

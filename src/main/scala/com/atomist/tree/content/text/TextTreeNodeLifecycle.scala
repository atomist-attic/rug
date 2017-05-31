package com.atomist.tree.content.text

import com.atomist.rug.kind.core.FileArtifactBackedMutableView
import com.atomist.tree.UpdatableTreeNode

object TextTreeNodeLifecycle {

  /**
    * This is how tree nodes progress:
    *
    * ┌───────────────────┐                    ┌───────────────────┐
    * │                   │                    │                   │
    * ───parsing────▶│PositionedTreeNode │─────makeReady─────▶│ UpdatableTreeNode │───────▶  proceed with
    * │                   │                    │                   │            updates
    * └───────────────────┘                    └───────────────────┘
    *
    * Parsing produces PositionedTreeNodes, which know their offset.
    * makeReady incorporates the original contents of the file, and also the file's MutableView,
    * so that the output UpdatableTreeNodes can report their value, accept a new value,
    * and propagate that value all the way back to the file.
    */

  /* This is for Antlr grammars, or anything that represents the entire file in a single parsed node. */
  def makeWholeFileNodeReady(typeName: String,
                             parsed: PositionedTreeNode,
                             fileArtifact: FileArtifactBackedMutableView,
                             preprocess: String => String = identity,
                             postprocess: String => String = identity): UpdatableTreeNode = {
    val inputAsParsed = preprocess(fileArtifact.content)
    val parsedWithWholeFileOffsets =
      ImmutablePositionedTreeNode(parsed).copy(
        startPosition = OffsetInputPosition(0),
        endPosition = OffsetInputPosition(inputAsParsed.length))
    makeReady(typeName, Seq(parsedWithWholeFileOffsets), fileArtifact, preprocess, postprocess).head
  }

  /**
    * Some or all of the file has been parsed into some PositionedTreeNodes.
    * (Since microgrammars aren't here anymore, it should always be ALL afaik, unless the
    * language extension chooses to return a node or nodes representing only part of the file.
    * It could happen.)
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
  def makeReady(typeName: String,
                matches: Seq[PositionedTreeNode],
                fileArtifact: FileArtifactBackedMutableView,
                preprocess: String => String = identity,
                postprocess: String => String = identity): Seq[UpdatableTreeNode] = {
    val inputAsParsed = preprocess(fileArtifact.content)

    val ottn: OverwritableTextTreeNode =
      ImmutablePositionedTreeNode.pad(typeName: String, matches, inputAsParsed)

    val wrapperNodeContainingWholeFileContent =
      new OverwritableTextInFile(typeName, ottn.allKidsIncludingPadding, postprocess)
    wrapperNodeContainingWholeFileContent.setParent(fileArtifact)
    wrapperNodeContainingWholeFileContent.childNodes.collect {
      case utn: UpdatableTreeNode => utn // should be all of them
      case _ => ???
    }
  }


}

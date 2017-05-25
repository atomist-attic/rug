package com.atomist.tree.content.text

import com.atomist.tree.TreeNode.{Noise, Significance}
import com.atomist.tree.{PaddingTreeNode, TreeNode}

import scala.collection.mutable.ListBuffer

case class ImmutablePositionedTreeNode(override val nodeName: String,
                                       override val startPosition: InputPosition,
                                       override val endPosition: InputPosition,
                                       override val childNodes: Seq[PositionedTreeNode],
                                       override val nodeTags: Set[String],
                                       override val significance: Significance)
  extends PositionedTreeNode {

  // These are really only needed on the Updatable tree nodes, not on Positioned at all

  override def value: String = ""

  override def childNodeNames: Set[String] = ???

  override def childNodeTypes: Set[String] = ???

  override def childrenNamed(key: String): Seq[TreeNode] = childNodes.filter(_.nodeName == key)
}

object ImmutablePositionedTreeNode {

  def apply(from: PositionedTreeNode): ImmutablePositionedTreeNode = {
    ImmutablePositionedTreeNode(
      from.nodeName,
      from.startPosition,
      from.endPosition,
      from.childNodes.collect { case ptn: PositionedTreeNode => ptn },
      from.nodeTags,
      from.significance)
  }

  def pad(typeName: String, pupae: Seq[PositionedTreeNode], initialSource: String): OverwritableTextTreeNode = {
    val wrapper = ImmutablePositionedTreeNode("wrapper",
      OffsetInputPosition(0),
      OffsetInputPosition(initialSource.length),
      pupae,
      Set(),
      TreeNode.Signal)
    val collapsed = collapseNoise(wrapper)
    val resolved = fightWithSiblings(collapsed)
    padInternal(resolved, initialSource)
  }

  private def collapseNoise(n: PositionedTreeNode): ImmutablePositionedTreeNode = {
    def collapseNoiseInternal(n: PositionedTreeNode): Seq[ImmutablePositionedTreeNode] = {
      val childrenWorthKeeping = n.childNodes.collect { case ptn: PositionedTreeNode => ptn }
        .flatMap(collapseNoiseInternal)
        .sortBy { tn => (tn.startPosition.offset, tn.endPosition.offset) }
      if (n.significance == Noise)
        childrenWorthKeeping
      else
        Seq(ImmutablePositionedTreeNode(n).copy(childNodes = childrenWorthKeeping))
    }

    val result = collapseNoiseInternal(n)
    if (result.size != 1)
      throw new IllegalArgumentException("Look, the top-level node must be Signal, not Noise")
    result.head
  }

  /*
   * It has been observed that sometimes nodes -- at least whitespace in Python -- have siblings
   * that are inside them.
   *
   * precondition: children are sorted by (startPosition, endPosition)
   * postcondition: siblings never overlap
   */
  private def fightWithSiblings(parent: ImmutablePositionedTreeNode): PositionedTreeNode = {

    if (parent.childNodes.isEmpty) parent else {
      val newChildren = parent.childNodes.sliding(2, 1).collect {
        case Seq(child1: ImmutablePositionedTreeNode, child2: PositionedTreeNode) =>
          val nextSiblingBegins = child2.startPosition
          if (nextSiblingBegins.offset < child1.endPosition.offset)
            fightWithSiblings(child1.copy(endPosition = nextSiblingBegins))
          else
            fightWithSiblings(child1)
      }.toSeq :+ fightWithSiblings(parent.childNodes.last.asInstanceOf[ImmutablePositionedTreeNode])
      parent.copy(childNodes = newChildren)
    }
  }

  private def padInternal(pupae: PositionedTreeNode, initialSource: String): OverwritableTextTreeNode = {

    def padding(from: Int, to: Int): TreeNode = {
      val name = s"padding[$from-$to]"
      val content = initialSource.substring(from, to)
      PaddingTreeNode(name, content)
    }

    val fieldResults = ListBuffer.empty[TreeNode]
    var lastEndOffset = pupae.startPosition.offset
    for {
      fv <- pupae.childNodes
    } {
      fv match {
        case sm: PositionedTreeNode =>
          if (sm.startPosition.offset > lastEndOffset) {
            fieldResults.append(padding(lastEndOffset, sm.startPosition.offset))
          }
          fieldResults.append(padInternal(sm, initialSource))
          lastEndOffset = sm.endPosition.offset
        case other =>
        // Ignore it. Let padding do its work
      }
    }
    // Put in trailing padding if necessary
    if (pupae.endPosition.offset > lastEndOffset) {
      fieldResults.append(padding(lastEndOffset, pupae.endPosition.offset))
    }

    OverwritableTextTreeNode(pupae, fieldResults)
  }

}

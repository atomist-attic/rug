package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.{InputPosition, MutableTerminalTreeNode, OffsetInputPosition, SimpleMutableContainerTreeNode}

/**
  * Match 0 or more occurrences of a node.
  * We create a new subnode with the given name
  *
  * @param m
  * @param name
  */
case class Rep(m: Matcher, name: String = "rep") extends Matcher {

  override def matchPrefix(offset: Int, s: CharSequence): Option[PatternMatch] =
    m.matchPrefix(offset, s) match {
      case None =>
        // We can match zero times. Put in an empty node.
        val pos = OffsetInputPosition(offset)
        Some(
          PatternMatch(node = Some(EmptyContainerTreeNode(name, pos)),
            offset = offset, matched = "", s, this.toString))
      case Some(there) =>
        // We matched once. Let's keep going
        var matched = there.matched
        val offset = there.offset
        var upToOffset = offset
        var nodes = there.node.toSeq
        while (m.matchPrefix(upToOffset, s) match {
          case None => false
          case Some(m) =>
            upToOffset = m.endPosition.offset
            nodes ++= m.node.toSeq
            true
        }) {
          // Do nothing
        }

        val pos = OffsetInputPosition(offset)
        val endpos = if (nodes.isEmpty) pos else nodes.last.endPosition
        val combinedNode = new SimpleMutableContainerTreeNode(name, nodes, pos, endpos)
        Some(
          PatternMatch(node = Some(combinedNode),
            offset, matched, s, this.toString)
        )
    }
}


object Repsep {

  def apply(m: Matcher, sep: Matcher, name: String): Matcher =
    Wrap(m.? ~? Rep(sep ~? m), name)

}

private case class EmptyContainerTreeNode(name: String, pos: InputPosition)
  extends MutableTerminalTreeNode(name, "", pos) {

  override def endPosition: InputPosition = startPosition

  override def nodeType: String = "empty"

  override def value: String = ""
}
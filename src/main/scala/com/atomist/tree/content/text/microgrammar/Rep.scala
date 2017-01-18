package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.{InputPosition, MutableTerminalTreeNode, OffsetInputPosition, SimpleMutableContainerTreeNode}

/**
  * Match 0 or more occurrences of a node.
  * We create a new subnode with the given name
  *
  * @param m         matcher that may match 0 or more times
  * @param name      name of the matcher
  * @param separator separator. If this is supplied, this is handled as a repsep rather than a straight rep
  */
case class Rep(m: Matcher, name: String = "rep", separator: Option[Matcher] = None)
  extends Matcher {

  private val secondaryMatch = separator match {
    case None => m
    case Some(sep) => Discard(sep) ~? m
  }

  override def matchPrefix(offset: Int, s: CharSequence): Option[PatternMatch] =
    m.matchPrefix(offset, s) match {
      case None =>
        // We can match zero times. Put in an empty node.
        val pos = OffsetInputPosition(offset)
        Some(
          PatternMatch(node = Some(EmptyContainerTreeNode(name, pos)),
            offset = offset, matched = "", s, this.toString))
      case Some(initialMatch) =>
        // We matched once. Let's keep going
        //println(s"Found initial match for $initialMatch")
        var matched = initialMatch.matched
        val offset = initialMatch.offset
        var upToOffset = initialMatch.endPosition.offset
        var nodes = initialMatch.node.toSeq
        //println(s"Trying secondary match $secondaryMatch against [${s.toString.substring(upToOffset)}]")
        while (secondaryMatch.matchPrefix(upToOffset, s) match {
          case None => false
          case Some(lastMatch) =>
            //println(s"Made it to secondary match [$lastMatch]")
            upToOffset = lastMatch.endPosition.offset
            matched += lastMatch.matched
            nodes ++= lastMatch.node.toSeq
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
    Rep(m, name, Some(sep))

}

private case class EmptyContainerTreeNode(name: String, pos: InputPosition)
  extends MutableTerminalTreeNode(name, "", pos) {

  override def endPosition: InputPosition = startPosition

  addType("empty")

  override def value: String = ""
}
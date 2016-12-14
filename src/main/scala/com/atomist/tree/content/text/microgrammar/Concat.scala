package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.ContainerTreeNode
import com.atomist.tree.content.text.SimpleMutableContainerTreeNode
import com.atomist.tree.content.text.microgrammar.PatternMatch.MatchedNode

object Concat {

  val DefaultConcatName = "concat"
}

/**
  * Concatenate two patterns
  *
  * @param left  left pattern
  * @param right right pattern
  */
case class Concat(left: Matcher, right: Matcher, name: String = Concat.DefaultConcatName)
  extends Matcher {

  override def matchPrefix(offset: Int, input: CharSequence): Option[PatternMatch] = {
    val l = left.matchPrefix(offset, input)
    l match {
      case None =>
        // We're done. It cannot match.
        None
      case Some(leftMatch) =>
        // So far so good
        right.matchPrefix(leftMatch.remainderOffset, input) match {
          case None =>
            // We're done. Right doesn't match.
            None
          case Some(rightMatch) =>
            val mergedTree: Option[MatchedNode] = (leftMatch.node, rightMatch.node) match {
              case (None, None) => None
              case (Some(l), None) => Some(l)
              case (None, Some(r)) => Some(r)
              case (Some(l), Some(r)) =>
                val mergedFields = (l match {
                  case ctn: ContainerTreeNode => ctn.childNodes
                  case n => Seq(n)
                }) ++ (r match {
                  case ctn: ContainerTreeNode => ctn.childNodes
                  case n => Seq(n)
                })
                Some(new SimpleMutableContainerTreeNode(name, mergedFields, l.startPosition, r.endPosition))
            }
            Some(PatternMatch(mergedTree, offset, leftMatch.matched + rightMatch.matched, input, this.toString))
        }
    }
  }
}

package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.ContainerTreeNode
import com.atomist.tree.content.text.SimpleMutableContainerTreeNode
import com.atomist.tree.content.text.microgrammar.PatternMatch.MatchedNode
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object Concat {

  val DefaultConcatName = ".concat"

  lazy val logger: Logger =
    Logger(LoggerFactory.getLogger(getClass.getName))
}

/**
  * Concatenate two patterns
  *
  * @param left  left pattern
  * @param right right pattern
  */
case class Concat(left: Matcher, right: Matcher, name: String = Concat.DefaultConcatName)
  extends Matcher {

  import Concat.logger

  override def matchPrefix(inputState: InputState): Option[PatternMatch] = {
    val l = left.matchPrefix(inputState)
    l match {
      case None =>
        // We're done. It cannot match.
        None
      case Some(leftMatch) =>
        // So far so good
        right.matchPrefix(leftMatch.resultingInputState) match {
          case None =>
            // We're done. Right doesn't match.
            logger.debug(s"We matched OK on [$left]->[${leftMatch}] but failed on [$right], next 20 characters were [${leftMatch.resultingInputState.take(20)}]")
            None
          case Some(rightMatch) =>
            // Both match.
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
            Some(PatternMatch(mergedTree,
              leftMatch.matched + rightMatch.matched,
              rightMatch.resultingInputState,
              this.toString))
        }
    }
  }
}

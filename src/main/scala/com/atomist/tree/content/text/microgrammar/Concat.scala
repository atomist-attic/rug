package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.ContainerTreeNode
import com.atomist.tree.content.text.microgrammar.Matcher.MatchPrefixResult
import com.atomist.tree.content.text.{PositionedTreeNode, SimpleMutableContainerTreeNode}
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

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult = {
    val l = left.matchPrefix(inputState)
    l match {
      case Left(no) =>
        // We're done. It cannot match.
        Left(no.andSo("left didn't match"))
      case Right(leftMatch) =>
        // So far so good
        right.matchPrefix(leftMatch.resultingInputState) match {
          case Left(noOnTheRight) =>
            // We're done. Right doesn't match.
            logger.debug(s"We matched OK on [$left]->[${leftMatch}] but failed on [$right], next 20 characters were [${leftMatch.resultingInputState.take(20)}]")
            Left(noOnTheRight.andSo("right didn't match"))
          case Right(rightMatch) =>
            // Both match.
            val mergedTree: Option[PositionedTreeNode] = (leftMatch.node, rightMatch.node) match {
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
            Right(PatternMatch(mergedTree,
              leftMatch.matched + rightMatch.matched,
              rightMatch.resultingInputState,
              this.toString))
        }
    }
  }
}

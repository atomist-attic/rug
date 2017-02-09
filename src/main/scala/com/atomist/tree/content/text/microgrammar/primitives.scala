package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.{MutableTerminalTreeNode, OffsetInputPosition, SimpleMutableContainerTreeNode}
import com.atomist.tree.content.text.microgrammar.Matcher.MatchPrefixResult
import com.atomist.tree.utils.TreeNodeUtils

/**
  * Try first to match the left pattern, then the right
  *
  * @param a left pattern
  * @param b right pattern
  */
case class Alternate(a: Matcher, b: Matcher, name: String = "alternate") extends Matcher {

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult = {
    a.matchPrefix(inputState) match {
      case Left(noFromA) =>
        b.matchPrefix(inputState).left.map(noFromB => DismatchReport("Neither matched", Seq(noFromA, noFromB)))
      case Right(leftMatch) =>
        Right(leftMatch)
    }
  }
}

/**
  * Match but discard the node output of the matcher
  *
  * @param m    matcher whose node result we'll discard
  * @param name name of this discarding matcher (useless)
  */
case class Discard(m: Matcher, name: String = "discard") extends Matcher {

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult =
    m.matchPrefix(inputState).right.map(matched => matched.copy(node = new MutableTerminalTreeNode(name, matched.node.value, matched.node.startPosition, significance = TreeNode.Noise)))

}

/**
  * Wrap the matcher with a new name and in a higher level container node
  *
  * @param m    matcher to wrap
  * @param name new name that will be used for the matcher and the new node level
  */
case class Wrap(m: Matcher, name: String)
  extends Matcher {

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult =
    m.matchPrefix(inputState).right.map {
      matched =>
        val wrappedNode =
          SimpleMutableContainerTreeNode.wrap(name, matched.node)
        matched.copy(node = wrappedNode)
    }
}

case class Optional(m: Matcher, name: String = Optional.DefaultOptionalName) extends Matcher {

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult = {
    val significance = if (name == Optional.DefaultOptionalName) TreeNode.Noise else TreeNode.Signal
      m.matchPrefix(inputState) match {
        case Left(no) =>
          Right(PatternMatch(EmptyContainerTreeNode(name, OffsetInputPosition(inputState.offset), significance =significance), matched = "", inputState, this.toString))
        case Right(there) =>
          Right(there)
      }
  }
}
object Optional {
  val DefaultOptionalName = ".optional"
}

package com.atomist.tree.content.text

import com.atomist.rug.kind.core.FileArtifactBackedMutableView
import com.atomist.source.StringFileArtifact
import com.atomist.tree.{AddressableTreeNode, PaddingTreeNode, ParentAwareTreeNode, TreeNode}
import com.atomist.tree.TreeNode.Noise
import com.atomist.tree.utils.TreeNodeUtils

import scala.collection.mutable.ListBuffer

/**
  * Connects OverwritableTextTreeNodes to the file they reside in.
  * This node holds the padding before and between and after them.
  * It also glues the updates together: it calls update on the FileArtifactMutableView
  * when any node under it gets updated.
  *
  * This should be invisible in the path expression tree.
  *
  * @param dynamicType the type represented by each of the nodes underneath.
  * @param allKids     the OverwritableTextTreeNodes
  */
class OverwritableTextInFile(dynamicType: String,
                             allKids: Seq[TreeNode])
  extends OverwritableTextTreeNodeParent {

  import OverwritableTextTreeNode._

  private var state: LifecyclePhase = Unready
  private var _value: String = allKids.map(_.value).mkString("")
  private val visibleChildren = allKids.filter(_.significance != Noise)

  def setParent(fmv: FileArtifactBackedMutableView): Unit = {
    if (state != Unready)
      throw new IllegalStateException("wat you can only set the parent once")
    fileView = fmv
    claimChildren()
    state = Ready
  }

  private[text] var fileView: FileArtifactBackedMutableView = _

  override def commit(): Unit = requireReady {
    _value = allKids.map(_.value).mkString("") // some child has changed
    fileView.updateTo(StringFileArtifact.updated(fileView.currentBackingObject, _value))
    fileView.commit()
  }

  override def address: String = fileView.address

  override def nodeName: String = s"set of matches on type $dynamicType"

  override def value: String = _value

  override def childNodes: Seq[TreeNode] = visibleChildren

  override def childNodeNames: Set[String] = visibleChildren.map(_.nodeName).toSet

  override def childNodeTypes: Set[String] = Set(dynamicType)

  override def childrenNamed(key: String): Seq[TreeNode] = visibleChildren.filter(_.nodeName == key)

  /**
    * Return the node at this position in the file, if known
    */
  def nodeAt(pos: InputPosition): Option[AddressableTreeNode] = {

    def inner(mostUsefulSoFar: Option[AddressableTreeNode], remainingOffset: Int, children: Seq[TreeNode]): Option[AddressableTreeNode] = {
      def sameValueAsContainingNodeAbove(tn: TreeNode) = mostUsefulSoFar.exists(_.value == tn.value)
      val firstChild = children.head
      val others = children.drop(1)
      firstChild match {
        case _ if firstChild.value.length <= remainingOffset => // this is not the child I'm looking for
          inner(mostUsefulSoFar, remainingOffset - firstChild.value.length, others)
        case lessInteresting : OverwritableTextTreeNode if sameValueAsContainingNodeAbove(lessInteresting) =>
          inner(mostUsefulSoFar, remainingOffset, lessInteresting.allKidsIncludingPadding)
        case moreInteresting : OverwritableTextTreeNode =>
          inner(Some(moreInteresting), remainingOffset, moreInteresting.allKidsIncludingPadding)
        case unaddressable =>
          mostUsefulSoFar
      }
    }

    inner(None, pos.offset, allKids)
  }

  /*
   * called by descendants to find their position in the file
   */
  def formatInfoFromHere(stringsToLeft: String, childAsking : OverwritableTextTreeNodeChild, valueOfInterest: String): FormatInfo = {
    def valueBefore(child: OverwritableTextTreeNodeChild) = allKids.takeWhile(_ != childAsking).map(_.value).mkString


    val stringToLeft = valueBefore(childAsking) + stringsToLeft
    val leftPoint = FormatInfo.contextInfo(stringToLeft)
    val rightPoint = FormatInfo.contextInfo(stringToLeft + valueOfInterest)
    require(value.startsWith(stringToLeft), s"Bad prefix calculating formatInfo [$stringToLeft] in [$value]")
    FormatInfo(leftPoint, rightPoint)
  }

  /* mine */

  private def requireReady[T](result: => T): T = {
    if (state != Ready)
      throw new IllegalStateException(s"This is only valid when the node is Ready but I am in $state")
    result
  }

  private def claimChildren(): Unit = allKids.foreach {
    case ch: OverwritableTextTreeNodeChild =>
      ch.setParent(this, determineLocationStep(visibleChildren, ch), this)
    case _ => // padding, whatever
  }

  private def determineLocationStep(visibleChildren: Seq[TreeNode], forChild: TreeNode): String = {
    if(allKids.size == 1) {
      // there's only one thing here. We parsed the whole file.
      s"$dynamicType()"
    } else {
      // there are multiple matches here. Index
      val childrenBeforeThis = visibleChildren.takeWhile(_ != forChild)
      val thisChildsIndex = childrenBeforeThis.size
      // XPath indexes from 1
      s"$dynamicType()[${thisChildsIndex + 1}]"
    }
  }
}

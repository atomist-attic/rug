package com.atomist.tree.content.text

import com.atomist.rug.spi.MutableView
import com.atomist.source.{FileArtifact, StringFileArtifact}
import com.atomist.tree.TreeNode
import com.atomist.tree.TreeNode.Noise

/**
  * Connects OverwritableTextTreeNodes to the file they reside in.
  * This node holds the padding before and between and after them.
  * It also glues the updates together: it calls update on the FileArtifactMutableView
  * when any node under it gets updated.
  *
  * This should be invisible in the path expression tree.
  *
  * @param dynamicType the type represented by each of the nodes underneath.
  * @param allKids the OverwritableTextTreeNodes
  */
class OverwritableTextInFile(dynamicType: String,
                             allKids: Seq[TreeNode])
  extends OverwritableTextTreeNodeParent {
  import OverwritableTextTreeNode._

  private var state: LifecyclePhase = Unready
  private var _value: String = allKids.map(_.value).mkString("")
  private val visibleChildren = allKids.filter(_.significance != Noise)

  def setParent(fmv: MutableView[FileArtifact]): Unit = {
    if (state != Unready)
      throw new IllegalStateException("wat you can only set the parent once")
    fileView = fmv
    claimChildren()
    state = Ready
  }

  private[text] var fileView: MutableView[FileArtifact] = _

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

  /*
   * called by descendants to find their position in the file
   */
  def formatInfo(child: TreeNode): FormatInfo = {
    def descs(f: OverwritableTextTreeNode): Seq[TreeNode] = {
      f +: f.allKidsIncludingPadding.flatMap {
        case d: OverwritableTextTreeNode => descs(d)
        case n => Seq(n)
      }
    }

    val descendants = this +: allKids.flatMap {
      case d: OverwritableTextTreeNode => descs(d)
      case n => Seq(n)
    }

    if (!descendants.contains(child))
      throw new IllegalStateException(s"I can't find my child: $child")
    else {
      // Build string to the left
      val leftFields = descendants.takeWhile(f => f != child)
      val stringToLeft = leftFields
        .filter(_.childNodes.isEmpty)
        .map(_.value).mkString("")
      val leftPoint = FormatInfo.contextInfo(stringToLeft)
      val rightPoint = FormatInfo.contextInfo(stringToLeft + child.value)
      //println(s"Found $fi from left string [$stringToLeft] with start=$start")
      FormatInfo(leftPoint, rightPoint)
    }
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
    val thisChildsName = forChild.nodeName
    val childrenBeforeThisWithTheSameName = visibleChildren.takeWhile(_ != forChild).filter(_.nodeName == thisChildsName)
    val thisChildsIndex = childrenBeforeThisWithTheSameName.size

    s"$dynamicType()[$thisChildsIndex]"
  }
}

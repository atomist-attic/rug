package com.atomist.tree.content.text

import com.atomist.rug.kind.core.FileArtifactBackedMutableView
import com.atomist.source.StringFileArtifact
import com.atomist.tree.{AddressableTreeNode, TreeNode}
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
  * @param allKids     the OverwritableTextTreeNodes
  */
class OverwritableTextInFile(dynamicType: String,
                             allKids: Seq[TreeNode],
                             postProcess: String => String)
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
    fileView.updateTo(StringFileArtifact.updated(fileView.currentBackingObject, postProcess(_value)))
    fileView.commit()
  }

  override def address: String = fileView.address

  override def nodeName: String = s"set of matches on type $dynamicType"

  override def value: String = _value

  override def childNodes: Seq[TreeNode] = visibleChildren

  override def childNodeNames: Set[String] = visibleChildren.map(_.nodeName).toSet

  override def childNodeTypes: Set[String] = Set(dynamicType)

  override def childrenNamed(key: String): Seq[TreeNode] = visibleChildren.filter(_.nodeName == key)

  private case class NodeAndPath(node: TreeNode, path: Seq[TreeNode])

  /**
    * All descendants of this node with position info, with their paths
    */
  private def descendants: Seq[NodeAndPath] = {
    def descs(f: OverwritableTextTreeNode, path: Seq[TreeNode]): Seq[NodeAndPath] = {
      NodeAndPath(f, path) +: f.allKidsIncludingPadding.flatMap {
        case d: OverwritableTextTreeNode => descs(d, path :+ d)
        case n => Seq(NodeAndPath(n, path))
      }
    }

    NodeAndPath(this, Nil) +: allKids.flatMap {
      case d: OverwritableTextTreeNode => descs(d, Seq(this))
      case n => Seq(NodeAndPath(n, Seq(this)))
    }
  }

  /**
    * Return the node at this position in the file, if known
    */
  private def nodeAndPathAt(pos: InputPosition): Option[NodeAndPath] = {
    val terminalDescendants = descendants.filter(np => np.node.childNodes.isEmpty)
    val stringToLeft = new StringBuilder
    val nodesBefore =
      for {
        n <- terminalDescendants
        if stringToLeft.size < pos.offset
      }
        yield {
          stringToLeft.append(n.node.value)
          n
        }
    nodesBefore.lastOption
  }

  /**
    * Try to find an addressable node at this position in the file
    */
  def nodeAt(pos: InputPosition): Option[AddressableTreeNode] = {
    nodeAndPathAt(pos) flatMap {
      case NodeAndPath(atb: AddressableTreeNode, _) => Some(atb)
      case NodeAndPath(_, path) =>
        (path collect {
          case atb: AddressableTreeNode => atb
        }).lastOption
    }
  }

  /**
    * Find the node at this position in the file
    */
  def rawNodeAt(pos: InputPosition): Option[TreeNode] = {
    nodeAndPathAt(pos).map(np => {
      np.node
    })
  }

  /*
   * called by descendants to find their position in the file
   */
  def formatInfo(child: TreeNode): FormatInfo = {
    val descs = descendants.map(_.node)
    if (!descs.contains(child))
      throw new IllegalStateException(s"I can't find my child: $child")
    else {
      // Build string to the left
      val leftFields = descs.takeWhile(f => f != child)
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
    // XPath indexes from 1
    s"$dynamicType()[${thisChildsIndex + 1}]"
  }
}

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

  private case class NodeAndPath(node: TreeNode, path: Seq[TreeNode])

  private def terminalDescendants: Seq[NodeAndPath] = {
    def descs(f: OverwritableTextTreeNode, path: Seq[TreeNode]): Seq[NodeAndPath] = {
      f.allKidsIncludingPadding.flatMap {
        case d: OverwritableTextTreeNode if d.childCount == 0 =>
          // Special case, it's hiding a padding node
          Seq(NodeAndPath(d, path))
        case d: OverwritableTextTreeNode =>
          descs(d, path :+ d)
        case n => Seq(NodeAndPath(n, path))
      }
    }

    allKids.flatMap {
      case d: OverwritableTextTreeNode =>
        descs(d, Seq(this))
      case n => Seq(NodeAndPath(n, Seq(this)))
    }
  }


  /**
    * Return the node at this position in the file, if known
    */
  private def nodeAndPathAt(pos: InputPosition): Option[NodeAndPath] = {
    val stringToLeft = new StringBuilder

    val leftFields = ListBuffer.empty[NodeAndPath]
    var done = false

    for {
      n <- terminalDescendants
      if !done
    } {
      if (stringToLeft.size < pos.offset)
        stringToLeft.append(n.node.value)
      else
      // Take the next one in this case
        done = true
      leftFields.append(n)
    }

    //println(leftFields.map(_.node).map(n => s"${n.nodeName}:${n.significance}:${n.getClass.getSimpleName}:[${n.value.take(50)}]").mkString("\n"))

    require(value.startsWith(stringToLeft), s"Bad prefix calculating nodeAndPathAt [$stringToLeft] in [$value]")
    // Back up to the most general node with this value if we're too specific
    leftFields.lastOption.map(np => {
      // We know there is one. We want the first one in the path with the same value
      val earliest = np.path.find(n => n.value == np.node.value)
      earliest.map(found => {
        NodeAndPath(found, np.path.takeWhile(n => n != found))
      }).getOrElse(np)
    })
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
    nodeAndPathAt(pos).map(np => np.node)
  }

  /*
   * called by descendants to find their position in the file
   */
  def formatInfo(targetNode: TreeNode): FormatInfo = {

    val terminals = terminalDescendants.map(_.node)

    //    if (false && !descendants.contains(targetNode))
    //      throw new IllegalStateException(s"I can't find target node descendant: $targetNode")

    // Build string to the left
    val leftFields = terminals.takeWhile(f => !(f == targetNode || TreeNodeOperations.isKnownAncestor(f, targetNode)))
    println(leftFields.map(n => s"${n.nodeName}:${n.significance}:${n.getClass.getSimpleName}:[${n.value.take(50)}]").mkString("\n"))

    val stringToLeft = leftFields
      .map(_.value).mkString("")
    val leftPoint = FormatInfo.contextInfo(stringToLeft)
    val rightPoint = FormatInfo.contextInfo(stringToLeft + targetNode.value)
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
    val thisChildsName = forChild.nodeName
    val childrenBeforeThisWithTheSameName = visibleChildren.takeWhile(_ != forChild).filter(_.nodeName == thisChildsName)
    val thisChildsIndex = childrenBeforeThisWithTheSameName.size
    // XPath indexes from 1
    s"$dynamicType()[${thisChildsIndex + 1}]"
  }
}

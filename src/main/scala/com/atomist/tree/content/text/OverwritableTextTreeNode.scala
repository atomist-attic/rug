package com.atomist.tree.content.text

import com.atomist.rug.kind.core.FileArtifactBackedMutableView
import com.atomist.rug.spi.{ExportFunction, TypeProvider, Typed}
import com.atomist.tree.TreeNode.{Noise, Signal}
import com.atomist.tree._

trait OverwritableTextTreeNodeChild {

  def setParent(p: OverwritableTextTreeNodeParent,
                locationStepIntoMe: String,
                rootNode: OverwritableTextInFile): Unit

  def invalidate(by: OverwritableTextTreeNodeParent): Unit // Should be only done by parents?

}

trait OverwritableTextTreeNodeParent extends TreeNode {

  def commit(): Unit // should be only done by children?

  def address: String

  def formatInfoFromHere(stringsToLeft: Seq[String], childAsking: OverwritableTextTreeNodeChild, valueOfInterest: String): FormatInfo

}

class OverwritableTextTypeProvider extends TypeProvider(classOf[OverwritableTextTreeNode]) {

  override def description: String = "Generic text container"
}

/**
  * Serves as a mutable, updatable-from-rug hierarchy
  *
  * does not need a MutableView over it
  *
  * @param name           This node is addressable by this in a path expression
  * @param allKids        children include Padding nodes and more OverwritableTextTreeNodes.
  * @param additionalTags This node is addressable by each of these, as tag()
  */
class OverwritableTextTreeNode(name: String,
                               allKids: Seq[TreeNode],
                               additionalTags: Set[String])
  extends UpdatableTreeNode
    with ContainerTreeNode // tag
    with AddressableTreeNode // we conform, but we implement this here
    with ParentAwareTreeNode
    with OverwritableTextTreeNodeChild
    with OverwritableTextTreeNodeParent {

  import OverwritableTextTreeNode._

  /* This hierarchy is constructed bottom-up, then hydrated with parent references.
   * When update() is called on a node, it replaces itself with that text
   * Invalidates everything below it
   * marks a change in everything above it.
   *
                     │
               construction
                     │
                     ▼
             ┌───────────────┐
             │               │
             │    Unready    │
             │               │
             └───────────────┘
                     │
               iAmYourFather
                     │
                     ▼
             ┌───────────────┐
             │               │
        ┌────│     Ready     │────┐
        │    │               │    │
        │    └───────────────┘    │
     update                  invalidate
        │                         │
        ▼                         ▼
┌───────────────┐         ┌───────────────┐
│               │         │               │
│  Overridden   │         │  Invalidated  │
│               │         │               │
└───────────────┘         └───────────────┘
   */

  private var state: LifecyclePhase = Unready
  private var _value: String = allKids.map(_.value).mkString

  private[text] val allKidsIncludingPadding = allKids

  private val visibleChildren = allKids.filter(_.significance != Noise)

  def setParent(p: OverwritableTextTreeNodeParent, locationStepIntoMe: String, rootNode: OverwritableTextInFile): Unit = {
    if (state != Unready)
      throw new IllegalStateException(s"Can only set parent once: $this")
    _parent = p
    this.rootNode = rootNode
    locationStep = locationStepIntoMe
    claimChildren(rootNode)
    state = Ready
  }

  private var _parent: OverwritableTextTreeNodeParent = _
  private var locationStep: String = _
  private var rootNode: OverwritableTextInFile = _

  def invalidate(by: OverwritableTextTreeNodeParent): Unit = requireReady {
    invalidator = by
    invalidateChildren(by)
    state = Invalidated
  }

  private var invalidator: OverwritableTextTreeNodeParent = _

  @ExportFunction(readOnly = false, description = "Update the whole value")
  override def update(to: String): Unit = requireReady {
    _value = to
    invalidateChildren(this)
    _parent.commit()
    state = Overwritten
  }

  /*
   * Normal TreeNode methods
   */
  override val nodeTags: Set[String] = additionalTags + TreeNode.Dynamic + Typed.typeToTypeName(classOf[OverwritableTextTreeNode])

  override val nodeName: String = name

  override val significance = Signal

  override def value: String = requireNotInvalidated(_value)

  override def childNodeNames: Set[String] = requireNotInvalidated(requireNotOverwritten(visibleChildren.map(_.nodeName).toSet))

  override def childNodeTypes: Set[String] = requireNotInvalidated(requireNotOverwritten(visibleChildren.flatMap(_.nodeTags).toSet))

  override def childrenNamed(key: String): Seq[TreeNode] = requireNotInvalidated(requireNotOverwritten(visibleChildren.filter(_.nodeName == key)))

  override def childNodes: Seq[TreeNode] = requireNotInvalidated(requireNotOverwritten(visibleChildren))

  override def address: String = requireParentalKnowledge(_parent.address + "/" + locationStep)

  override def commit(): Unit = requireReady {
    _value = allKids.map(_.value).mkString("") // some child has changed
    _parent.commit()
  }

  @ExportFunction(readOnly = true, description = "Return the parent node")
  def parent: TreeNode = {
    _parent match {
      case o: OverwritableTextInFile => o.fileView
      case _ => _parent
    }
  }

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    exposeResultDirectlyToNashorn = true,
    description = "Return the format info for the start of this structure in the file or null if not available")
  final def formatInfo: FormatInfo =
    requireNotInvalidated(_parent.formatInfoFromHere(Seq(), this, value))

  def formatInfoFromHere(stringsSoFar: Seq[String], childAsking: OverwritableTextTreeNodeChild, valueOfInterest: String): FormatInfo = {
    def valueBefore(child: OverwritableTextTreeNodeChild) = allKidsIncludingPadding.takeWhile(_ != childAsking).map(_.value).mkString
    _parent.formatInfoFromHere(valueBefore(childAsking) +: stringsSoFar, this, valueOfInterest)
  }

  def nodeAt(pos: InputPosition): Option[TreeNode] =
    rootNode.nodeAt(pos)

  def file: FileArtifactBackedMutableView =
    rootNode.fileView

  override def toString: String =
    if (state == Unready)
      s"unready ${super.toString}"
    else {
      val showValue = if (childCount == 0) s"value=[$value]" else s"length=${value.length}"
      s"OverwritableTextTreeNode:name=[$name];$showValue"
    }

  /* mine */
  private def requireReady[T](result: => T): T =
    requireNotInvalidated {
      if (state != Ready)
        throw new IllegalStateException(s"This is only valid when the node is Ready but I am in $state")
      result
    }

  private def requireNotInvalidated[T](result: => T): T = {
    if (state == Invalidated)
      throw new OutOfDateNodeException(s"This node has been overwritten by ${invalidator.address}") // could print my address, highlighting where this one is in it
    result
  }

  private def requireNotOverwritten[T](result: => T): T = {
    if (state == Overwritten)
      throw new OutOfDateNodeException(s"This node's value has been overwritten.")
    result
  }

  private def requireParentalKnowledge[T](result: => T): T = {
    if (state == Unready) {
      throw new IllegalStateException(s"This node is not ready yet! Who is my father??")
    }
    result
  }

  private def invalidateChildren(by: OverwritableTextTreeNodeParent) =
    allKids.foreach {
      case ch: OverwritableTextTreeNodeChild => ch.invalidate(by)
      case _ => // meh, padding
    }

  private def claimChildren(rootNode: OverwritableTextInFile): Unit = allKids.foreach {
    case ch: OverwritableTextTreeNodeChild =>
      ch.setParent(this, determineLocationStep(visibleChildren, ch), rootNode)
    case _ => // padding, whatever
  }
}

object OverwritableTextTreeNode {
  def apply(ptn: PositionedTreeNode, fields: Seq[TreeNode]): OverwritableTextTreeNode = {
    new OverwritableTextTreeNode(ptn.nodeName, fields, ptn.nodeTags)
  }

  sealed trait LifecyclePhase

  case object Unready extends LifecyclePhase

  case object Ready extends LifecyclePhase

  case object Overwritten extends LifecyclePhase

  case object Invalidated extends LifecyclePhase

  /*
   * Tell the child its piece of its address.
   * There is an assumption here that our children remain the same and their names remain the same,
   * for this address to continue to work as children are updated.
   */
  private def determineLocationStep(visibleChildren: Seq[TreeNode], forChild: TreeNode): String = {
    val thisChildsName = forChild.nodeName
    if (!visibleChildren.contains(forChild))
      throw new IllegalStateException(s"I can't find my child $forChild")
    else {
      val childrenBeforeThisWithTheSameName = visibleChildren.takeWhile(_ != forChild).filter(_.nodeName == thisChildsName)
      val thisChildsIndex = childrenBeforeThisWithTheSameName.size
      // XPath indexes from 1
      s"$thisChildsName[${thisChildsIndex + 1}]"
    }
  }
}

class OutOfDateNodeException(msg: String) extends RuntimeException(msg)
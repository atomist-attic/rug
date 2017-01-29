package com.atomist.tree.content.text

import com.atomist.tree.utils.TreeNodeUtils
import com.atomist.tree._

import scala.collection.mutable.ListBuffer

/**
  * Implementation of MutableContainerTreeNode that holds position information and allows addition,
  * removal and replacement of fields.
  *
  * @param nodeName name of the node
  */
abstract class PositionedMutableContainerTreeNode(val nodeName: String)
  extends MutableContainerTreeNode
    with PositionedPaddableTreeNode {

  def endPosition: InputPosition

  private[text] var _fieldValues = ListBuffer.empty[TreeNode]

  private var _padded = false

  protected def markPadded(): Unit = _padded = true

  override def padded: Boolean = _padded

  final override def childNodes: Seq[TreeNode] =
    if (padded) fieldValues.filterNot(n => n.significance == TreeNode.Noise)
    else fieldValues

  override def fieldValues: Seq[TreeNode] = _fieldValues

  override def childNodeNames: Set[String] = childNodes.map(f => f.nodeName).toSet

  override def formatInfo(child: TreeNode): Option[FormatInfo] = {
    if (!_padded)
      throw new IllegalStateException(s"Call pad before trying to get format info from $this")
    else
      super.formatInfo(child)
  }

    // TODO is this right
  override def childNodeTypes: Set[String] = childNodeNames

  override def childrenNamed(key: String): Seq[TreeNode] = fieldValues.filter(n => n.nodeName == key)

  override def pad(initialSource: String, isNoise: TreeNode => Boolean, topLevel: Boolean = false): Unit =
    if (!_padded) {
      removeNoise(isNoise)
      val (hatched, _) = PositionedMutableContainerTreeNode.pad(this, initialSource, topLevel)
      _fieldValues = ListBuffer.empty[TreeNode]
      _fieldValues.append(hatched.fieldValues: _*)
      _padded = true
    }


  // We're not yet padded. Screen out noise
  private def removeNoise(isNoise: TreeNode => Boolean): Unit = {
    if (padded)
      throw new IllegalArgumentException(s"Cannot remove noise from $this as it's already padded")
    val newFieldValues = ListBuffer.empty[TreeNode]
    for {
      f <- _fieldValues
    } {
      newFieldValues.appendAll(collapse(isNoise, f))
    }
    _fieldValues = newFieldValues
  }

  /**
    * Get rid of this level, pulling up its children
    */
  private def collapse(isNoise: TreeNode => Boolean, n: TreeNode): Seq[TreeNode] = {
    val collapse2: TreeNode => Seq[TreeNode] = collapse(isNoise, _)
    if (isNoise(n)) n match {
      case mtn: MutableContainerTreeNode =>
        // Pull up the children
        // We need to look in the field values, not the child nodes, as the child nodes will be noise free
        val collapsed = mtn.fieldValues.flatMap(collapse2)
        //println(s"COLLAPSED ${n.nodeName}:${n.nodeTags}(${n.childNodes.size}) into ${collapsed.mkString}")
        collapsed
      case _ => Nil
    }
    else {
      //println(s"Not collapsing ${n.nodeName}:${n.nodeTags}")
      Seq(n)
    }
  }

  /**
    * Replace all the fields of this object with the new fields
    *
    * @param newFields fields to replace existing fields with
    */
  protected def replaceFields(newFields: Seq[TreeNode]): Unit = {
    _fieldValues.clear()
    _fieldValues.appendAll(newFields)
  }

  /**
    * This allows parsed fields to be added in any order. Position will be checked
    * to ensure that they're each added in the right place.
    */
  def insertFieldCheckingPosition(newField: TreeNode): Unit = newField match {
    case np: PositionedTreeNode if np.startPosition != null =>
      var inserted = false
      for {
        existingField <- _fieldValues
        if !inserted
      }
        existingField match {
          case ep: PositionedTreeNode if ep.startPosition != null && np.startPosition.offset < ep.startPosition.offset =>
            addFieldBefore(newField, ep)
            inserted = true
          case _ =>
        }
      if (!inserted)
        _fieldValues.append(newField)
    case _ =>
      appendField(newField)
  }

  override def appendField(newField: TreeNode): Unit = {
    _fieldValues.append(newField)
  }

  def replaceField(old: TreeNode, newFields: TreeNode): Unit = {
    val index = _fieldValues.indexOf(old)
    if (index < 0)
      throw new IllegalArgumentException(s"Can't replace $old as it's not a known field")
    _fieldValues(index) = newFields
  }

  def removeField(old: TreeNode): Unit = {
    _fieldValues -= old
  }

  /**
    * Convenience method to add a field just before the last field.
    * If there are no existing fields, just append it.
    */
  def addFieldBeforeLast(newField: TreeNode): Unit = {
    if (_fieldValues.isEmpty)
      appendField(newField)
    else {
      val lastIndex = _fieldValues.size - 1
      _fieldValues.insert(lastIndex, newField)
    }
  }

  def addFieldBefore(newField: TreeNode, before: TreeNode): Unit = {
    val idx = _fieldValues.indexOf(before)
    if (idx < 0)
      throw new IllegalArgumentException(s"Can't add a field before $before as it's not a known field")
    _fieldValues.insert(idx, newField)
  }

  def addFieldAfter(after: TreeNode, newField: TreeNode): Unit = {
    val idx = _fieldValues.indexOf(after)
    if (idx < 0)
      throw new IllegalArgumentException(s"Can't add a field after $after as it's not a known field")
    if (after == _fieldValues.last)
      _fieldValues.append(newField)
    else
      _fieldValues.insert(idx + 1, newField)
  }

  private var dirtied = false

  override def dirty: Boolean = super.dirty || dirtied

  override def update(to: String): Unit = {
    replaceFields(Seq(SimpleTerminalTreeNode(nodeName, to)))
    dirtied = true
  }

  final override def value: String = {
    if (!_padded)
      throw new IllegalStateException(s"Call pad before getting value from $this")
    _fieldValues.map(_.value).mkString("")
  }

  override def toString =
    s"${
      getClass.getSimpleName
    }($nodeName)[$startPosition-$endPosition]: {${
      childNodes.mkString(",")
    }}"
}


object PositionedMutableContainerTreeNode {
  type Report = Seq[String]


  def pad(pupae: PositionedTreeNode, initialSource: String, topLevel: Boolean = false): (MutableContainerTreeNode, Report) = {
    var report: Seq[String] = Seq()

    def say(s: String) = report = s +: report

    // Number of characters of fields to show in padding field names
    val show = 40

    def padding(from: Int, to: Int): TreeNode = {
      //      require(from >= 0 && from < initialSource.size, s"from for padding must be 0-${initialSource.size}, had $from")
      //      require(to >= 0 && to < initialSource.size, s"from for padding must be 0-${initialSource.size}, $to")

      val content = initialSource.substring(from, to)
      val name = s"$from-$to[${
        val pcontent = TreeNodeUtils.inlineReturns(content)
        if (pcontent.length > show) s"${pcontent.take(show)}..." else pcontent
      }]"
      val pad = PaddingTreeNode(name, content)
      pad
    }

    val fieldResults = ListBuffer.empty[TreeNode]
    if (pupae.startPosition == null)
      throw new IllegalStateException(s"startPosition not set in $pupae.nodeName: class $this")

    // There may be content before the first production that we want to account if we are a top level production
    if (topLevel && pupae.startPosition.offset > 0) {
      fieldResults.append(padding(0, pupae.startPosition.offset))
    }

    say(s"Processing ${pupae.childNodes.length} children")
    var lastEndOffset = pupae.startPosition.offset
    for {
      fv <- pupae.childNodes
    } {
      fv match {
        case sm: PositionedPaddableTreeNode if sm.initialized =>
          // This condition isn't pretty, is it? No. I suspect we are not testing the right conditions.
          // Perhaps we mean "if this node isn't a bunch of whitespace" (that's a thing in Python)
          // but that whitespace appears to be appended to the previous node maybe? because if we add those
          // whitespace nodes here it gets doubled.
          if (sm.startPosition.offset >= lastEndOffset || sm.isInstanceOf[PositionedMutableContainerTreeNode]) {
            try {
              sm.pad(initialSource)
            }
            catch {
              case iex: IllegalArgumentException =>
                throw new IllegalArgumentException(s"Cannot find position when processing $this and trying to pad $sm", iex)
            }
            val smoffset = sm.startPosition.offset
            if (smoffset > lastEndOffset) fieldResults.append(padding(lastEndOffset, smoffset))
            lastEndOffset = sm.endPosition.offset
            fieldResults.append(sm)
          }
          else {
            say(s"Skipping this mutable terminal tree node. ${sm.startPosition} and lastEndOffset is ${lastEndOffset}")
          }
        case sm: PositionedTreeNode =>
          if (sm.startPosition.offset >= lastEndOffset) {
            val smoffset = sm.startPosition.offset
            if (smoffset > lastEndOffset) fieldResults.append(padding(lastEndOffset, smoffset))
            lastEndOffset = sm.endPosition.offset
            fieldResults.append(sm)
          }
          else {
            say(s"Skipping this mutable terminal tree node. ${sm.startPosition} and lastEndOffset is ${lastEndOffset}")
          }
        case f: TreeNode if "".equals(f.value) =>
          // It's harmless. Keep it as it may be queried. It won't be updateable
          // Because we probably don't know where it lives.
          fieldResults.append(f)
          say(s"Keeping empty node $f")
        case other =>
          say(s"Ignoring node: $other")
        // Ignore it. Let padding do its work
      }
    }
    // Put in trailing padding if necessary
    if (pupae.endPosition.offset > lastEndOffset) {
      fieldResults.append(padding(lastEndOffset, pupae.endPosition.offset))
    }

    // If it's a top level element, make sure we account for the entire file
    if (topLevel) {
      val content = initialSource.substring(pupae.endPosition.offset)
      if (content.nonEmpty) {
        val pn = PaddingTreeNode("End", content)
        fieldResults.append(pn)
      }
    }
    (new MutableButNotPositionedContainerTreeNode(pupae.nodeName, fieldResults), report)
  }
}

/**
  * Conceptually:
  * We parse PositionedTreeNodes, and then we pad them and they become MutableTreeNodes.
  * PositionedTreeNodes are like pupa, and the pad method hatches them into moths.
  * The moths can fly around and be useful; you can do things with a MutableContainerTreeNode
  * like get its value! and update it!
  *
  * But don't do those things before padding!
  *
  * This class represents the MutableTreeNode I'd like to have. Right now PositionedTreeNode
  * and MutableTreeNode are conflated, and the pad method actually updates the instance.
  *
  * As of its creation, it will only be used for tests.
  */
class MutableButNotPositionedContainerTreeNode(
                                                name: String,
                                                val initialFieldValues: Seq[TreeNode]
                                              )
  extends PositionedMutableContainerTreeNode(name) {

  initialFieldValues.foreach(insertFieldCheckingPosition)

  override def childrenNamed(key: String): Seq[TreeNode] = fieldValues.filter(n => n.nodeName.equals(key))

  markPadded()

  var endPosition: InputPosition = _

  var startPosition: InputPosition = _
}
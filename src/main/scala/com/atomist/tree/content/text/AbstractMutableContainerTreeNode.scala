package com.atomist.tree.content.text

import com.atomist.tree.utils.TreeNodeUtils
import com.atomist.tree.{PaddingNode, SimpleTerminalTreeNode, TreeNode}

import scala.collection.mutable.ListBuffer

/**
  * Implementation of MutableContainerTreeNode that holds position information and allows addition,
  * removal and replacement of fields.
  *
  * @param nodeName name of the node
  */
abstract class AbstractMutableContainerTreeNode(
                                                 val nodeName: String
                                               )
  extends MutableContainerTreeNode
    with PositionedTreeNode {

  def endPosition: InputPosition

  private[text] var _fieldValues = ListBuffer.empty[TreeNode]

  private var _padded = false

  def padded = _padded

  final override def childNodes: Seq[TreeNode] = _fieldValues

  override def nodeType: String = "mutable"

  override def fieldValues = _fieldValues

  // TODO is this right
  override def childNodeTypes: Set[String] = childNodeNames

  /**
    * Tell this node it doesn't need padding
    */
  def markPadded(): Unit = {
    _padded = true
  }

  /**
    * Compile this so that we can manipulate it at will without further
    * reference to the input string.
    * Introduces padding objects to cover string content that isn't explained in known structures.
    * Must be called before value method is invoked.
    *
    * @param initialSource entire source
    * @param topLevel      whether this is a top level element, in which
    *                      case we should pad after known structures
    */
  def pad(initialSource: String, topLevel: Boolean = false): Unit = if (!_padded) {

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
      val pad = PaddingNode(name, content)
      pad
    }

    val fieldResults = ListBuffer.empty[TreeNode]
    if (startPosition == null)
      throw new IllegalStateException(s"startPosition not set in $nodeName: class $this")

    // There may be content before the first production that we want to account if we are a top level production
    if (topLevel && startPosition.offset > 0) {
      fieldResults.append(padding(0, startPosition.offset))
    }

    var lastEndOffset = startPosition.offset
    for {
      fv <- _fieldValues
    } {
      fv match {
        case sm: MutableTerminalTreeNode if sm.startPosition != null && sm.startPosition.offset >= 0 && sm.startPosition.offset >= lastEndOffset =>
          val smoffset = sm.startPosition.offset
          if (smoffset > lastEndOffset) fieldResults.append(padding(lastEndOffset, smoffset))
          lastEndOffset = sm.endPosition.offset
          fieldResults.append(sm)
        case soo: AbstractMutableContainerTreeNode =>
          try {
            soo.pad(initialSource)
          }
          catch {
            case iex: IllegalArgumentException =>
              throw new IllegalArgumentException(s"Cannot find position when processing $this and trying to pad $soo", iex)
          }
          val smoffset = soo.startPosition.offset
          if (smoffset > lastEndOffset) fieldResults.append(padding(lastEndOffset, smoffset))
          lastEndOffset = soo.endPosition.offset
          fieldResults.append(soo)
        case f if "".equals(f.value) =>
          // It's harmless. Keep it as it may be queried. It won't be updateable
          // Because we probably don't know where it lives.
          fieldResults.append(f)
        case _ =>
        // Ignore it. Let padding do its work
      }
    }
    // Put in trailing padding if necessary
    if (endPosition.offset > lastEndOffset) {
      fieldResults.append(padding(lastEndOffset, endPosition.offset))
    }

    // If it's a top level element, make sure we account for the entire file
    if (topLevel) {
      val content = initialSource.substring(endPosition.offset)
      if (content.nonEmpty) {
        val pn = PaddingNode("End", content)
        fieldResults.append(pn)
        // assertPaddingInvariants(initialSource)
      }
    }
    this._fieldValues = fieldResults
    _padded = true
  }

  private def assertPaddingInvariants(initialSource: String): Unit = {
    val lastFieldValue = this._fieldValues.last.value
    val endOfInput = initialSource.takeRight(_fieldValues.last.value.length.min(initialSource.length))
    require(lastFieldValue.equals(endOfInput),
      s"Last field value of [$lastFieldValue] does not equal end of input string, [$endOfInput]")
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
    *
    * @param newField
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
    *
    * @param newField
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

  def length: Int = endPosition - startPosition

  private var dirtied = false

  override def dirty = super.dirty || dirtied

  override def update(to: String): Unit = {
    replaceFields(Seq(SimpleTerminalTreeNode(nodeName, to)))
    dirtied = true
  }

  final override def value: String = {
    if (!_padded)
      throw new IllegalStateException(s"Call pad before getting value from $this")
    _fieldValues.map(f => f.value).mkString("")
  }

  override def toString =
    s"${getClass.getSimpleName}($nodeName)[$startPosition-$endPosition]: {${childNodes.mkString(",")}}"
}

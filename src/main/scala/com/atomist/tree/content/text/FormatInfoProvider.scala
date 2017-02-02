package com.atomist.tree.content.text

import com.atomist.rug.spi.ExportFunction
import com.atomist.tree.TreeNode

/**
  * Extended by classes that can expose their position
  */
trait FormatInfoProvider {

  @ExportFunction(readOnly = true,
    description = "Return the format info for the start of this structure in the file or null if not available")
  final def formatInfoStart: FormatInfo =
    rootNode.flatMap(r => r.formatInfoStart(focusNode)).orNull

  @ExportFunction(readOnly = true,
    description = "Return the format info for the end of this structure in the file or null if not available")
  final def formatInfoEnd: FormatInfo =
    rootNode.flatMap(r => r.formatInfoEnd(focusNode)).orNull

  /**
    * Focus node
    */
  protected def focusNode: TreeNode

  /**
    * Root container
    */
  protected def rootNode: Option[PositionedMutableContainerTreeNode]

}

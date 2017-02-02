package com.atomist.tree.content.text

import com.atomist.rug.spi.ExportFunction
import com.atomist.tree.TreeNode

/**
  * Extended by classes that can expose their position
  */
trait FormatInfoProvider {

  @ExportFunction(readOnly = true,
    description = "Return the format info for the start of this structure in the file or null if not available")
  final def formatInfo: FormatInfo =
    rootNode.flatMap(r => r.formatInfo(focusNode)).orNull

  /**
    * Focus node
    */
  protected def focusNode: TreeNode

  /**
    * Root container
    */
  protected def rootNode: Option[MutableContainerTreeNode]

}

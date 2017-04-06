package com.atomist.tree.content.text

import com.atomist.rug.spi.ExportFunction
import com.atomist.tree.TreeNode

trait FormatInfoProvider {

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    exposeResultDirectlyToNashorn = true,
    description = "Return the format info for the start of this structure in the file or null if not available")
  def formatInfo: FormatInfo

}

/**
  * Extended by classes that can expose their position.
  */
trait FormatInfoProviderSupport extends FormatInfoProvider {

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    exposeResultDirectlyToNashorn = true,
    description = "Return the format info for the start of this structure in the file or null if not available")
  override final def formatInfo: FormatInfo =
    rootNode.flatMap(_.formatInfo(focusNode)).orNull

  /**
    * Focus node.
    */
  protected def focusNode: TreeNode

  /**
    * Root container.
    */
  protected def rootNode: Option[MutableContainerTreeNode]

}

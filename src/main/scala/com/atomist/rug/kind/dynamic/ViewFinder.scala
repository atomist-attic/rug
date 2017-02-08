package com.atomist.rug.kind.dynamic

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.parser._
import com.atomist.rug.spi.MutableView
import com.atomist.source.ArtifactSource
import com.atomist.tree.TreeNode
import org.springframework.util.ObjectUtils

/**
  * Try to find children of this type in the given context
  */
trait ChildResolver {

  /**
    * Find all in this context
    *
    * @param context
    */
  def findAllIn(context: TreeNode): Option[Seq[TreeNode]]
}

/**
  * Find views in a context, with Project knowledge. Used to drive `with` block execution.
  */
trait ViewFinder extends ChildResolver {

}

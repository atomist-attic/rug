package com.atomist.rug.kind.grammar

import com.atomist.rug.kind.core._
import com.atomist.rug.kind.dynamic.{MutableContainerMutableView, MutableTreeNodeUpdater}
import com.atomist.rug.runtime.rugdsl.DefaultEvaluator
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.FileArtifact
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.content.text.grammar.MatchListener

import scala.collection.JavaConverters._

/**
  * Convenient superclass for types that parse file content and can
  * be resolved from files and projects
  */
abstract class TypeUnderFile extends Type(DefaultEvaluator)
  with ReflectivelyTypedType {

  /**
    * Is this file of interest to this type? Typically will involve an extension check
    *
    * @param f file to test
    * @return whether we should try to parse the file with our parser
    */
  def isOfType(f: FileArtifact): Boolean

  override def runtimeClass: Class[_] = classOf[MutableContainerMutableView]

  override def findAllIn(context: TreeNode): Option[Seq[TreeNode]] = context match {
      case pmv: ProjectMutableView =>
        Some(pmv
          .files
          .asScala
          .filter(f => isOfType(f.currentBackingObject))
          .flatMap(f => toView(f))
        )
      case f: FileMutableView if isOfType(f.currentBackingObject) =>
        Some(toView(f).toSeq)
      case _ => None
    }

  private def toView(f: FileArtifactBackedMutableView): Option[MutableView[_]] = {
    val rawNode = fileToRawNode(f.currentBackingObject)
    rawNode.map(n => {
      val mtn = createView(n, f)
      // Ensure the file is updated based on any changes to the underlying AST at any level
      mtn match {
        case m: MutableContainerMutableView =>
          f.registerUpdater(new MutableTreeNodeUpdater(m.currentBackingObject))
        case _ =>
      }
      mtn
    })
  }

  /**
    * Subclasses can override this if they want to customize the top level node created:
    * for example, to add verbs that can be used instead of drilling into path expressions.
    *
    * @return new mutable view
    */
  protected def createView(n: MutableContainerTreeNode, f: FileArtifactBackedMutableView): MutableView[_] = {
    new MutableContainerMutableView(n, f)
  }

  /**
    * Return a parsed node. Useful to validate content, for example in tests.
    *
    * @param f file with content to parse
    * @return
    */
  def fileToRawNode(f: FileArtifact, ml: Option[MatchListener] = None): Option[MutableContainerTreeNode]

}

package com.atomist.rug.kind.grammar

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core._
import com.atomist.rug.kind.dynamic.MutableContainerMutableView
import com.atomist.rug.runtime.rugdsl.DefaultEvaluator
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.FileArtifact
import com.atomist.tree.content.text._
import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.{TreeNode, UpdatableTreeNode}

import scala.collection.JavaConverters._

/**
  * Convenient superclass for types that parse file content and can
  * be resolved from files and projects
  */
abstract class TypeUnderFile
  extends Type(DefaultEvaluator)
    with ReflectivelyTypedType {

  /**
    * Is this file of interest to this type? Typically will involve an extension check
    *
    * @param f file to test
    * @return whether we should try to parse the file with our parser
    */
  def isOfType(f: FileArtifact): Boolean

  override def runtimeClass: Class[_] = classOf[MutableContainerMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[TreeNode]] = context match {
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

  private def toView(f: FileArtifactBackedMutableView): Option[TreeNode] = {
    val inner = fileToRawNode(f.currentBackingObject) match {
      case Some(ptn: PositionedTreeNode) =>
        Some(TextTreeNodeLifecycle.makeWholeFileNodeReady(name, ptn, f))
      case None => None
      case x => throw new RuntimeException(s"What is $x")
    }
    inner.map(createView(_, f))
  }

  /**
    * Subclasses can override this if they want to customize the top level node created:
    * for example, to add verbs that can be used instead of drilling into path expressions.
    *
    * @return new mutable view
    */
  protected def createView(tn: UpdatableTreeNode, f: FileArtifactBackedMutableView): TreeNode = tn match {
    case ottn: OverwritableTextTreeNode => ottn
    case n: MutableView[_] => n // this might not be necessary
    case n: MutableContainerTreeNode => new MutableContainerMutableView(n, f)
  }

  /**
    * Return a parsed node. Useful to validate content, for example in tests.
    *
    * @param f file with content to parse
    * @return
    */
  def fileToRawNode(f: FileArtifact, ml: Option[MatchListener] = None): Option[PositionedTreeNode]
}
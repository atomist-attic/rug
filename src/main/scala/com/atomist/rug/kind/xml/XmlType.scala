package com.atomist.rug.kind.xml

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.tree.TreeNode

class XmlType(
               evaluator: Evaluator
             )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def description = "XML file"

  override def viewManifest: Manifest[XmlMutableView] = manifest[XmlMutableView]

  override def findAllIn(context: TreeNode): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject.allFiles
          .filter(f => f.name.endsWith(".xml"))
          .map(f => new XmlMutableView(f, pmv))
        )
      case _ => None
    }
  }
}

package com.atomist.rug.kind.test

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectiveStaticTypeInformation, Type, TypeInformation}
import com.atomist.tree.TreeNode

import scala.reflect.ManifestFactory

class ReplacerCljType(ev: Evaluator) extends Type(ev) {

  def this() = this(DefaultEvaluator)

  def viewManifest: Manifest[_] = ManifestFactory.classType(viewClass)

  def typeInformation: TypeInformation = new ReflectiveStaticTypeInformation(viewClass)

  def description = "Test type for replacing the content of clojure files"

  protected def viewClass: Class[StringReplacingMutableView] = classOf[StringReplacingMutableView]

  protected def listViews(context: TreeNode): Seq[MutableView[_]] = context match {
    case pmv: ProjectMutableView =>
      pmv.currentBackingObject.allFiles.filter(f => f.path.contains(".clj"))
        .map(f => new StringReplacingMutableView(f, pmv))
    case _ => Nil
  }

  override def findAllIn(context: TreeNode): Option[Seq[MutableView[_]]] = Option(listViews(context))
}

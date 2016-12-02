package com.atomist.rug.kind.test

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectiveStaticTypeInformation, Type, TypeInformation}
import com.atomist.source.ArtifactSource

import scala.reflect.ManifestFactory

class StringReplacingTestTypeForClojure(ev: Evaluator) extends Type(ev) {

  def this() = this(DefaultEvaluator)

  def viewManifest: Manifest[_] = ManifestFactory.classType(viewClass)

  def typeInformation: TypeInformation = new ReflectiveStaticTypeInformation(viewClass)

  def name = "replacerclj"

  def description = "Test type for replacing the content of clojure files"

  protected def viewClass: Class[StringReplacingMutableView] = classOf[StringReplacingMutableView]

  protected def listViews(rugAs: ArtifactSource, selected: Selected,
                          context: MutableView[_], poa: ProjectOperationArguments,
                          identifierMap: Map[String, AnyRef]): Seq[MutableView[_]] = context match {
    case pmv: ProjectMutableView =>
      pmv.currentBackingObject.allFiles.filter(f => f.path.contains(".clj"))
        .map(f => new StringReplacingMutableView(f, pmv))
    case _ => Nil
  }

  def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_],
                poa: ProjectOperationArguments, identifierMap: Map[String, AnyRef]): Option[Seq[MutableView[_]]] = {
    val l = listViews(rugAs, selected, context, poa, identifierMap)
    Option.apply(l)
  }
}

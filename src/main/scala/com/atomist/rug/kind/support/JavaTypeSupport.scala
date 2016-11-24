package com.atomist.rug.kind.support

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.Evaluator
import com.atomist.rug.spi.{MutableView, ReflectiveStaticTypeInformation, Type, TypeInformation}
import com.atomist.source.ArtifactSource

import scala.collection.Seq
import scala.collection.immutable.Map
import scala.reflect.{Manifest, ManifestFactory}

abstract class JavaTypeSupport(ev: Evaluator) extends Type(ev) {

  def viewManifest: Manifest[_] = ManifestFactory.classType(viewClass)

  protected def viewClass: Class[_]

  def typeInformation: TypeInformation = new ReflectiveStaticTypeInformation(viewClass)

  def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_],
                poa: ProjectOperationArguments,
                identifierMap: Map[String, AnyRef]): Option[Seq[MutableView[_]]] = null
}
